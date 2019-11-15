package com.alexsmaliy.yesimdb.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCrawlerConfiguration.Builder.class)
public interface CrawlerConfiguration {
    @JsonProperty("crawler-threads")
    @Value.Default
    default int crawlerThreads() {
        return 4;
    }
}
