package com.jobskillsmatcher.ingest.impl.client;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JSearchClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500L;

    private final RestClient jsearchRestClient;
    private final Bucket jsearchRateLimiter;


    public List<JSearchJob> search(String query, String country, int page, int numPages) {
        if (!jsearchRateLimiter.tryConsume(1)) {
            throw new JSearchRateLimitedException("JSearch local quota exhausted");
        }
        HttpServerErrorException lastServerError = null;
        HttpClientErrorException lastTooMany = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                JSearchResponse response = jsearchRestClient.get()
                        .uri(uri -> uri.path("/search")
                                .queryParam("query", query)
                                .queryParam("page", page)
                                .queryParam("num_pages", numPages)
                                .queryParamIfPresent("country", optional(country))
                                .build())
                        .retrieve()
                        .body(JSearchResponse.class);
                return response == null ? List.of() : response.safeData();
            } catch (HttpClientErrorException.TooManyRequests ex) {
                lastTooMany = ex;
                sleepBackoff(attempt);
            } catch (HttpServerErrorException ex) {
                lastServerError = ex;
                sleepBackoff(attempt);
            }
        }

        if (lastTooMany != null) {
            throw new JSearchRateLimitedException("JSearch upstream returned 429 after retries");
        }
        throw new JSearchUnavailableException(
                "JSearch upstream failed after " + MAX_ATTEMPTS + " attempts",
                lastServerError);
    }

    private static java.util.Optional<String> optional(String value) {
        return value == null || value.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(value);
    }

    private static void sleepBackoff(int attempt) {
        long sleep = BASE_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static class JSearchRateLimitedException extends RuntimeException {
        public JSearchRateLimitedException(String message) {
            super(message);
        }
    }

    public static class JSearchUnavailableException extends RuntimeException {
        public JSearchUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
