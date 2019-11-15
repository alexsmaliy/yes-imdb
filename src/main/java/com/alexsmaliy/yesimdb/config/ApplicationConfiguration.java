package com.alexsmaliy.yesimdb.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableApplicationConfiguration.Builder.class)
public interface ApplicationConfiguration {
    @Value.Default
    default LuceneConfiguration lucene() {
        return new ImmutableLuceneConfiguration.Builder().build();
    }

    @Value.Default
    default CrawlerConfiguration crawler() {
        return new ImmutableCrawlerConfiguration.Builder().build();
    }
}
