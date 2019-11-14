package com.alexsmaliy.yesimdb.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class YesImdbConfiguration extends Configuration {
    @NotNull
    private ApplicationConfiguration applicationConfiguration;

    @JsonProperty("application")
    public ApplicationConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    @JsonProperty("application")
    public void setApplicationConfiguration(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }
}
