package com.jobskillsmatcher.ingest.impl.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobskillsmatcher.jsearch")
public record JSearchProperties(
        String baseUrl,
        String apiKey,
        RateLimit rateLimit
) {
    public record RateLimit(int capacity, int refillPerDay) { }
}
