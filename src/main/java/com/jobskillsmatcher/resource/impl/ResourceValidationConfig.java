package com.jobskillsmatcher.resource.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ResourceValidationConfig {

    // Dedicated RestClient for URL liveness probes. 5s timeout keeps one slow host from stalling the batch.
    @Bean
    RestClient validationRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(5);
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "JobSkillsMatcher-LinkChecker/1.0")
                .build();
    }
}
