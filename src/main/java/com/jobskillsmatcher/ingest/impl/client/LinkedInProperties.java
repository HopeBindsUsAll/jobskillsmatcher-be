package com.jobskillsmatcher.ingest.impl.client;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "jobskillsmatcher.linkedin")
public record LinkedInProperties(
        String baseUrl,
        String apiKey,
        String path,
        int limit,
        RateLimit rateLimit
) {
    public record RateLimit(int capacity, int refillPerDay) { }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public int effectiveLimit() {
        return limit > 0 ? limit : 25;
    }
}
