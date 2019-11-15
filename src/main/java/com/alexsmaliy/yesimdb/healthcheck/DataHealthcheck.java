package com.alexsmaliy.yesimdb.healthcheck;

import com.alexsmaliy.yesimdb.service.scraper.ScraperResource;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

public class DataHealthcheck extends HealthCheck {
    private static final String TEST_QUERY = "name:spielberg AND title:s*";
    private static final List<String> EXPECTED_SPIELBERG = ImmutableList.of(
        "Bridge of Spies",
        "Empire of the Sun",
        "Saving Private Ryan",
        "Schindler's List",
        "Shrek"
    );

    private final ScraperResource scraperResource;

    public DataHealthcheck(ScraperResource scraperResource) {
        this.scraperResource = scraperResource;
    }

    @Override
    protected Result check() {
        List<String> observedSpielberg = scraperResource.query(TEST_QUERY);
        Collections.sort(observedSpielberg);
        if (observedSpielberg.equals(EXPECTED_SPIELBERG)) {
            return Result.healthy();
        } else {
            return Result.unhealthy(
                String.format(
                    "Expected the test search to return %s, but it returned %s!",
                    EXPECTED_SPIELBERG, observedSpielberg));
        }
    }
}
