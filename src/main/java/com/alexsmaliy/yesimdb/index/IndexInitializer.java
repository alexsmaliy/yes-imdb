package com.alexsmaliy.yesimdb.index;

import com.google.common.collect.ImmutableMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class IndexInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexInitializer.class);

    private IndexInitializer() { /* utility class */ }

    public static IndexWriter getIndexWriter(Path rootIndexDir, String indexName) {
        IndexWriterConfig indexWriterConfig = getIndexWriterConfig();
        try {
            Path indexDir = getCanonicalIndexPath(rootIndexDir, indexName);
            Directory indexDirectory = FSDirectory.open(indexDir);
            return new IndexWriter(indexDirectory, indexWriterConfig);
        } catch (IOException e) {
            LOGGER.error("Unable to initialize index writer!", e);
            throw new RuntimeException("Unable to initialize index writer!", e);
        }
    }

    public static SearcherManager getSearcherManager(IndexWriter indexWriter,
                                                     @Nullable SearcherFactory searcherFactory) {
        try {
            return new SearcherManager(indexWriter, searcherFactory);
        } catch (IOException e) {
            LOGGER.error("Unable to initialize index searcher!", e);
            throw new RuntimeException("Unable to initialize index searcher!", e);
        }
    }

    private static Path getCanonicalIndexPath(Path root, String indexName) {
        return root.resolve(indexName);
    }

    private static IndexWriterConfig getIndexWriterConfig() {
        Analyzer defaultFieldAnalyzer = new WhitespaceAnalyzer();
        Map<String, Analyzer> fieldToAnalyzerMap = ImmutableMap.<String, Analyzer>builder()
            .put("name", new NameAnalyzer())
            .put("title", new ClassicAnalyzer())
            .build();
        Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultFieldAnalyzer, fieldToAnalyzerMap);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setMaxBufferedDocs(50);
        config.setCommitOnClose(true);
        return config;
    }
}
