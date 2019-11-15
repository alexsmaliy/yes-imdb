package com.alexsmaliy.yesimdb.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.nio.file.Path;
import java.nio.file.Paths;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableLuceneConfiguration.Builder.class)
public interface LuceneConfiguration {
    @JsonProperty("indexes-root-dir")
    @Value.Default
    default Path indexesRootDir() {
        return Paths.get("indexes");
    }

    @JsonProperty("max-results-per-query")
    @Value.Default
    default int maxResultsPerQuery() {
        return 100;
    }
}
