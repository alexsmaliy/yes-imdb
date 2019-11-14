package com.alexsmaliy.yesimdb.service.scraper;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableStartCrawl.class)
@JsonDeserialize(as = ImmutableStartCrawl.class)
@JsonTypeInfo(
    use = Id.NAME,
    include = As.PROPERTY
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ImmutableStartCrawl.class, name = "start-crawl"),
})
public interface StartCrawl extends ScraperCommand {}
