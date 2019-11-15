## What This Is


This is a small Dropwizard service that scrapes the
[top 1000 movies on IMDB](https://www.imdb.com/search/title/?groups=top_1000&sort=user_rating&view=simple)
and exposes a simple search API. The scraped data is indexed using Apache
Lucene, which runs in-process. The code is built and packaged using Gradle.

## How to Run It

Building and running this application requires Java 8+. Run the bootstrap script
`./gradlew run` from the root of the repository. This will:
- download Gradle (the actual build tool)
- download all code dependencies
- compile the codebase
- start the server in the current terminal window

The server starts without data. See the API section below for how to start a
crawl and search the data.

## API


### Start a Crawl

```bash
curl -XPOST localhost:8080/scraper/admin \
  --data '{"@type": "start-crawl"}' \
  -H "Content-Type: application/json"
```

This command:
- deletes the currently indexed data
- scrapes the IMDB site (using `application.crawler.crawler-threads` threads)
- indexes the movie titles and the names of all associated cast and crew

This command is asynchronous. The request returns immediately. The progress of
the crawl can be observed by following `log/scraper-service.log`.

If the crawl encounters an error, the server attempts to revert the changes.

Only one crawl is allowed to take place at any given time.

Searches will return partial results while a crawl is in progress.

### Search the Data

```bash
curl -XPOST localhost:8080/scraper/query \
  --data QUERY \
  -H "Content-Type: application/json"
```

This command searches all currently indexed data and returns the titles of
movies that match `QUERY`. Two search fields are available: `name` (names of
cast and crew) and `title` (movie titles). If no field is specified, we search
by name.

A range of queries can be parsed.
- `title:TERM`: titles containing TERM
- `name:TERM`: titles that are associated with people whose names contain TERM
- `title:TERM1 AND name:TERM2`: conjunctive search
- `TERM?`, `TERM*`: wildcard search

See the [Lucene query reference](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
for others.

## Configuration

Some aspects of this application are configurable. See
[server.yml](./server.yml) to configure:
- the number of threads to use while crawling (default 4)
- the maximum number of results per query (default 100)
- ports (default 8080 for the API and 8081 for the admin web interface)

The application also accepts the full range of
[Dropwizard configuration options](https://www.dropwizard.io/en/stable/manual/configuration.html).

The server must be restarted for configuration changes to take effect.

## Healthchecks

Visit the admin interface in a web browser (defaults to
[localhost:8081](http://localhost:8081)) when the server is running to see the
server healthcheck status, active threads, etc.
