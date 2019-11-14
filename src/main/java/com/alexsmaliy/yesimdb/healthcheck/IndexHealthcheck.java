package com.alexsmaliy.yesimdb.healthcheck;

import com.alexsmaliy.yesimdb.index.IndexableFields;
import com.alexsmaliy.yesimdb.index.ManagedIndex;
import com.codahale.metrics.health.HealthCheck;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;

public class IndexHealthcheck extends HealthCheck {
    private final ManagedIndex index;

    public IndexHealthcheck(ManagedIndex index) {
        this.index = index;
    }

    @Override
    protected Result check() throws Exception {
        SearcherManager searcherManager = index.getSearcherManager();
        IndexSearcher indexSearcher = null;
        try {
            indexSearcher = searcherManager.acquire();
            int docCount = indexSearcher.getIndexReader().getDocCount(IndexableFields.TITLE_FIELD);
            if (docCount == 1000) {
                return Result.healthy();
            } else {
                return Result.unhealthy(
                    String.format(
                        "The document count is only %d, not 1000 as expected!",
                        docCount));
            }
        } finally {
            if (indexSearcher != null) {
                searcherManager.release(indexSearcher);
            }
        }
    }
}
