package com.jobskillsmatcher.ingest.impl.client;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(LinkedInProperties.class)
public class LinkedInConfig {

    @Bean
    RestClient linkedInRestClient(LinkedInProperties props) {
        log.info("LinkedIn job-search client configured (enabled={}, path={}, limit={})",
                props.configured(), props.path(), props.effectiveLimit());
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("X-RapidAPI-Host", hostFromBaseUrl(props.baseUrl()))
                .defaultHeader("X-RapidAPI-Key", props.apiKey() == null ? "" : props.apiKey())
                .build();
    }

    @Bean
    Bucket linkedInRateLimiter(LinkedInProperties props) {
        int capacity = Math.max(props.rateLimit() == null ? 100 : props.rateLimit().capacity(), 1);
        int refillPerDay = Math.max(
                props.rateLimit() == null ? 100 : props.rateLimit().refillPerDay(), 1);
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillPerDay, Duration.ofDays(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static String hostFromBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String stripped = baseUrl.replaceFirst("^https?://", "");
        int slash = stripped.indexOf('/');
        return slash >= 0 ? stripped.substring(0, slash) : stripped;
    }
}
