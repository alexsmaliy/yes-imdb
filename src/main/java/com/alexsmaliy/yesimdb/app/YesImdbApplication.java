package com.alexsmaliy.yesimdb.app;

import com.alexsmaliy.yesimdb.config.YesImdbConfiguration;
import com.alexsmaliy.yesimdb.healthcheck.DataHealthcheck;
import com.alexsmaliy.yesimdb.healthcheck.IndexHealthcheck;
import com.alexsmaliy.yesimdb.index.Indexes;
import com.alexsmaliy.yesimdb.index.ManagedIndex;
import com.alexsmaliy.yesimdb.logging.InvalidDefinitionExceptionMapper;
import com.alexsmaliy.yesimdb.logging.JsonMappingExceptionMapper;
import com.alexsmaliy.yesimdb.persistence.LuceneIndexDirManager;
import com.alexsmaliy.yesimdb.service.scraper.ScraperResource;
import com.alexsmaliy.yesimdb.service.scraper.ScraperService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class YesImdbApplication extends Application<YesImdbConfiguration> {
    public static void main(String[] args) throws Exception {
        new YesImdbApplication().run(args);
    }

    @Override
    public void run(YesImdbConfiguration configuration, Environment environment) {
        /* INITIALIZE DISK PERSISTENCE */
        environment.lifecycle().manage(new LuceneIndexDirManager(configuration));

        /* REQUEST HANDLERS */
        int crawlerThreads = configuration.getApplicationConfiguration()
            .crawler()
            .crawlerThreads();
        ThreadFactory tf = new ThreadFactoryBuilder().setDaemon(true)
            .setNameFormat("crawler")
            .build();
        ExecutorService executor = environment.lifecycle()
            .executorService("crawler", tf)
            .minThreads(crawlerThreads)
            .maxThreads(crawlerThreads)
            .workQueue(new ArrayBlockingQueue<>(1001)) // 1000 movies + 1 admin job
            .build();
        Path indexDirPath = configuration.getApplicationConfiguration()
            .lucene()
            .indexesRootDir();
        ManagedIndex primaryIndex = new ManagedIndex(indexDirPath, Indexes.PRIMARY_INDEX_NAME);
        int maxResultsPerQuery = configuration.getApplicationConfiguration()
            .lucene()
            .maxResultsPerQuery();
        ScraperResource scraperResource =
            new ScraperService(executor, primaryIndex, maxResultsPerQuery);
        environment.jersey().register(scraperResource);

        /* APPLICATION HEALTHCHECKS */
        environment.healthChecks().register(
            "index-healthcheck",
            new IndexHealthcheck(primaryIndex));
        environment.healthChecks().register(
            "test-query-healthcheck",
            new DataHealthcheck(scraperResource));

        /* ADDITIONAL EXCEPTION HANDLING */
        environment.jersey().register(new InvalidDefinitionExceptionMapper());
        environment.jersey().register(new JsonMappingExceptionMapper());
    }

    @Override
    public void initialize(Bootstrap<YesImdbConfiguration> bootstrap) {
        // nothing so far
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
