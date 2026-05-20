package com.jobskillsmatcher.ingest.impl.groq;

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
@EnableConfigurationProperties(GroqProperties.class)
public class GroqConfig {

    @Bean
    RestClient groqRestClient(GroqProperties props) {
        log.info("Groq client configured (enabled={}, model={})", props.enabled(), props.model());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.baseUrl() == null ? "https://api.groq.com" : props.baseUrl())
                .defaultHeader("Content-Type", "application/json");
        if (props.apiKey() != null && !props.apiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.apiKey());
        }
        return builder.build();
    }

    @Bean
    Bucket groqRateLimiter(GroqProperties props) {
        int capacity = Math.max(props.rateLimit() == null ? 30 : props.rateLimit().capacity(), 1);
        int refillPerMinute = Math.max(
                props.rateLimit() == null ? 30 : props.rateLimit().refillPerMinute(), 1);
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
