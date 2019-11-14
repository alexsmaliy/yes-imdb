package com.alexsmaliy.yesimdb.api;

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
}
