package com.alexsmaliy.yesimdb.index;

import io.dropwizard.lifecycle.Managed;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.SearcherManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class ManagedIndex implements Managed {
    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;
    private final String indexName;

    public ManagedIndex(Path rootDirPath, String indexName) {
        indexWriter = IndexInitializer.getIndexWriter(rootDirPath, indexName);
        searcherManager = IndexInitializer.getSearcherManager(indexWriter, null);
        this.indexName = indexName;
    }

    @Override
    public void start() {}

    @Override
    public void stop() throws IOException {
        searcherManager.close();
        indexWriter.close();
    }

    public IndexWriter getIndexWriter() {
        return this.indexWriter;
    }

    public SearcherManager getSearcherManager() {
        return this.searcherManager;
    }

    public String getIndexName() {
        return this.indexName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedIndex that = (ManagedIndex) o;
        return indexName.equals(that.indexName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName);
    }
}
