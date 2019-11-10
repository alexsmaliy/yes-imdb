package com.alexsmaliy.yesimdb.app;

import com.alexsmaliy.yesimdb.index.ManagedIndex;
import com.alexsmaliy.yesimdb.logging.InvalidDefinitionExceptionMapper;
import com.alexsmaliy.yesimdb.logging.JsonMappingExceptionMapper;
import com.alexsmaliy.yesimdb.persistence.LuceneIndexDirManager;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.nio.file.Path;

public class YesImdbApplication extends Application<YesImdbConfiguration> {
    public static final String PRIMARY_INDEX_NAME = "primary-index";
    public static final int MAX_RESULTS_TO_RETURN = 5;

    public static void main(String[] args) throws Exception {
        new YesImdbApplication().run(args);
    }

    @Override
    public void run(YesImdbConfiguration configuration, Environment environment) {
        environment.lifecycle().manage(new LuceneIndexDirManager(configuration));
        Path indexDirPath = configuration.getApplicationConfiguration().lucene().indexesRootDir();
        ManagedIndex managedIndex = new ManagedIndex(indexDirPath, PRIMARY_INDEX_NAME);

        /* CUSTOMIZED EXCEPTION HANDLING */
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
