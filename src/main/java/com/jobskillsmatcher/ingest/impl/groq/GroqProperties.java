package com.jobskillsmatcher.ingest.impl.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "jobskillsmatcher.groq")
public record GroqProperties(
        String baseUrl,
        String apiKey,
        String model,
        boolean enabled,
        RateLimit rateLimit
) {
    public record RateLimit(int capacity, int refillPerMinute) { }

    public boolean usable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
