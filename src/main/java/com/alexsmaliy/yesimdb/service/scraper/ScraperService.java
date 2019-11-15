package com.alexsmaliy.yesimdb.service.scraper;

import com.alexsmaliy.yesimdb.index.ImmutableMovieIdentifier;
import com.alexsmaliy.yesimdb.index.IndexableFields;
import com.alexsmaliy.yesimdb.index.ManagedIndex;
import com.alexsmaliy.yesimdb.index.MovieIdentifier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ScraperService implements ScraperResource {
    private static final String IMDB_BASE_URL = "https://www.imdb.com";
    private static final String IMDB_SEARCH_PAGE_PATH = "/search/title";
    private static final String TOP_1000_QUERY_STRING = "/?groups=top_1000&sort=user_rating&view=simple";

    private static final QueryParser DEFAULT_QUERY_PARSER =
        new QueryParser(IndexableFields.CAST_CREW_NAME_FIELD, new WhitespaceAnalyzer());

    private static final Logger LOGGER = LoggerFactory.getLogger(ScraperService.class);

    private final ExecutorService executor;
    private final ManagedIndex index;
    private final AtomicBoolean scraperIsRunning;
    private final int maxResultsPerQuery;

    public ScraperService(ExecutorService executor, ManagedIndex index, int maxResultsPerQuery) {
        this.executor = executor;
        this.index = index;
        this.scraperIsRunning = new AtomicBoolean(false);
        this.maxResultsPerQuery = maxResultsPerQuery;
    }

    @Override
    public String admin(ScraperCommand request) {
        if (request instanceof StartCrawl) {
            // Only one full scrape should run at a time.
            if (scraperIsRunning.get()) {
                throw new BadRequestException(
                    "The crawler is already scraping IMDB. "
                        + "Please wait for it to finish!");
            } else {
                executor.submit(this::scrapeImdbAndIndexResults);
                return "You can monitor the crawler's progress via [scraper-service.log].";
            }
        }
        return "Unknown command!";
    }

    @Override
    public List<String> query(String queryString) {
        IndexSearcher searcher = null;
        List<String> movieResults = new ArrayList<>();
        try {
            Query query = tryParseQuery(queryString);
            searcher = tryAcquireSearcher();
            IndexReader indexReader = searcher.getIndexReader();
            TopDocs results = trySearch(searcher, query, maxResultsPerQuery);
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                try {
                    List<IndexableField> fields = indexReader.document(scoreDoc.doc).getFields();
                    fields.forEach(f -> movieResults.add(f.stringValue()));
                } catch (IOException e) {
                    LOGGER.error("Unable to load a search result [{}]! The user will get partial results!",
                        scoreDoc.doc, e);
                }
            }
            Collections.sort(movieResults);
            return movieResults;
        } finally {
            tryReleaseSearcher(searcher);
        }
    }

    private void tryReleaseSearcher(IndexSearcher indexSearcher) {
        try {
            index.getSearcherManager().release(indexSearcher);
        } catch (IOException e) {
            LOGGER.error("Failed to release index searcher for primary index after use!", e);
            throw new ServerErrorException(
                "Server error while searching!",
                Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Query tryParseQuery(String queryString) {
        try {
            return DEFAULT_QUERY_PARSER.parse(queryString);
        } catch (ParseException e) {
            LOGGER.error("Unable to parse user-submitted query!", e);
            throw new BadRequestException("Unable to parse your query!");
        }
    }

    private IndexSearcher tryAcquireSearcher() {
        try {
            SearcherManager searcherManager = index.getSearcherManager();
            searcherManager.maybeRefresh();
            return searcherManager.acquire();
        } catch (IOException e) {
            LOGGER.error("Failed to acquire an index searcher!", e);
            throw new ServerErrorException(
                "Server error while searching!",
                Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private TopDocs trySearch(IndexSearcher indexSearcher, Query query, int maxResults) {
        try {
            return indexSearcher.search(query, maxResults);
        } catch (IOException e) {
            LOGGER.error("Failed to search!", e);
            throw new ServerErrorException(
                "Server error while searching!",
                Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void scrapeImdbAndIndexResults() {
        LOGGER.info("Starting full crawl.");
        scraperIsRunning.set(true);

        try {
            // Clear existing data.
            LOGGER.info("Deleting previous data...");
            index.getIndexWriter().deleteAll();

            // Build the list of top 1000 movies.
            List<MovieIdentifier> top1000 = downloadListOfTop1000();

            // Build the list of crawler tasks, one per movie webpage.
            List<Callable<Void>> tasks = top1000.stream()
                .map(movie -> (Callable<Void>) () -> {
                        List<String> names = downloadNamesForMovie(movie);
                        index(movie, names);
                        LOGGER.debug("Crawled and indexed [{}]!", movie.title());
                        return null; // Java needs an explicit return in Callable.
                    }
                ).collect(ImmutableList.toImmutableList());

            // Submit crawler tasks, then invoke them to trigger any nested exceptions.
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }

            // Finalize any remaining changes to the index.
            index.getIndexWriter().commit();
        } catch (ExecutionException | FailedToDownloadSomethingException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Exception while crawling!", e);
            try {
                index.getIndexWriter().rollback();
                LOGGER.warn("Successfully rolled back index changes after error.");
            } catch (IOException nested) {
                LOGGER.error(
                    "Exception while rolling back changes after error while crawling!",
                    nested);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to modify index!", e);
            try {
                index.getIndexWriter().rollback();
                LOGGER.warn("Successfully rolled back index changes after error.");
            } catch (IOException nested) {
                LOGGER.error("Exception while rolling back changes after error while modifying index!",
                    nested);
            }
        } finally {
            scraperIsRunning.set(false);
            LOGGER.info("Finished crawl job.");
        }
    }

    private void index(MovieIdentifier movie, List<String> associatedNames) throws IOException {
        IndexWriter indexWriter = index.getIndexWriter();
        // Disambiguate between org.jsoup.nodes.Document and org.apache.lucene.document.Document.
        org.apache.lucene.document.Document toIndex = new org.apache.lucene.document.Document();
        IndexableField textField =
            new TextField(IndexableFields.TITLE_FIELD, movie.title(), Field.Store.YES);
        toIndex.add(textField);
        for (String name : associatedNames) {
            IndexableField nameField =
                new TextField(IndexableFields.CAST_CREW_NAME_FIELD, name, Field.Store.NO);
            toIndex.add(nameField);
        }
        indexWriter.addDocument(toIndex);
    }

    private List<MovieIdentifier> downloadListOfTop1000() throws FailedToDownloadSomethingException {
        Document doc = downloadHtml(IMDB_BASE_URL + IMDB_SEARCH_PAGE_PATH + TOP_1000_QUERY_STRING);
        ImmutableList.Builder<MovieIdentifier> builder = ImmutableList.builder();
        while (true) {
            for (Element e : doc.select("span[class=lister-item-header]")) {
                Element item = e.selectFirst("a[href]");
                String title = item.text();
                String url = item.attr("href");
                MovieIdentifier movie = ImmutableMovieIdentifier.builder()
                    .title(title)
                    .url(IMDB_BASE_URL + url)
                    .build();
                builder.add(movie);
            }

            // We reached the last page.
            Element nextPageLink =
                doc.selectFirst("a[class='lister-page-next next-page']");
            if (nextPageLink == null) {
                break;
            }

            doc = downloadHtml(IMDB_BASE_URL + nextPageLink.attr("href"));
        }
        return builder.build();
    }

    private Document downloadHtml(String url) throws FailedToDownloadSomethingException {
        try {
            return Jsoup.connect(url)
                .header("Accept", "text/html")
                .userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:70.0) Gecko/20100101 Firefox/70.0")
                .maxBodySize(3_000_000)
                .timeout((int) TimeUnit.SECONDS.toMillis(10))
                .get();
        } catch (IOException ioe) {
            throw new FailedToDownloadSomethingException(
                String.format("Failed to download URL [ %s ]!", url),
                ioe);
        }
    }

    private List<String> downloadNamesForMovie(MovieIdentifier movieId) throws FailedToDownloadSomethingException {
        String fullCastAndCrewUrl = movieId.fullCreditsUrl();
        Document movieHtml = downloadHtml(fullCastAndCrewUrl);
        Stream<String> nonCastNames = movieHtml.select("td[class=name]")
            .stream()
            .map(Element::text)
            .map(String::toLowerCase);
        Element castList = movieHtml.selectFirst("table[class=cast_list]");
        Stream<String> castNames = castList.select("td[class=primary_photo] + td")
            .stream()
            .map(Element::text)
            .map(String::toLowerCase);
        return Streams.concat(castNames, nonCastNames)
            .collect(ImmutableList.toImmutableList());
    }

    private static class FailedToDownloadSomethingException extends IOException {
        public FailedToDownloadSomethingException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }
}
