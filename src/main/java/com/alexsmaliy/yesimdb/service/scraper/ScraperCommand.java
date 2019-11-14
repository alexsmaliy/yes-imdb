package com.alexsmaliy.yesimdb.service.scraper;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(
    use = Id.NAME,
    include = As.PROPERTY
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StartCrawl.class, name = "start-crawl"),
})
public interface ScraperCommand {}
