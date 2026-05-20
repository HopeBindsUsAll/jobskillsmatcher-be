package com.jobskillsmatcher.ingest.impl.client;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class LinkedInJobClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500L;

    private final RestClient linkedInRestClient;
    private final Bucket linkedInRateLimiter;
    private final LinkedInProperties props;


    public List<JSearchJob> search(String query, String locationFilter) {
        int limit = props.effectiveLimit();
        if (!props.configured()) {
            throw new LinkedInUnavailableException(
                    "LinkedIn API key not configured (set RAPIDAPI_KEY)", null);
        }
        if (!linkedInRateLimiter.tryConsume(1)) {
            throw new LinkedInRateLimitedException("LinkedIn local quota exhausted");
        }
        HttpServerErrorException lastServerError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                LinkedInJob[] response = linkedInRestClient.get()
                        .uri(uri -> uri.path(props.path())
                                .queryParam("limit", limit)
                                .queryParam("offset", 0)
                                .queryParam("title_filter", query == null ? "" : query)
                                .queryParam("description_type", "text")
                                .queryParamIfPresent("location_filter", optional(locationFilter))
                                .build())
                        .retrieve()
                        .body(LinkedInJob[].class);
                return map(response);
            } catch (HttpClientErrorException.TooManyRequests ex) {
                sleepBackoff(attempt);
            } catch (HttpServerErrorException ex) {
                lastServerError = ex;
                sleepBackoff(attempt);
            }
        }
        throw new LinkedInUnavailableException(
                "LinkedIn upstream failed after " + MAX_ATTEMPTS + " attempts", lastServerError);
    }

    private static List<JSearchJob> map(LinkedInJob[] response) {
        if (response == null || response.length == 0) {
            return List.of();
        }
        List<JSearchJob> out = new ArrayList<>(response.length);
        for (LinkedInJob li : response) {
            if (li == null || li.id() == null || li.id().isBlank()) {
                continue;
            }
            boolean remote = li.locationType() != null
                    && li.locationType().equalsIgnoreCase("TELECOMMUTE");
            out.add(new JSearchJob(
                    li.id(),
                    li.title(),
                    li.organization(),
                    li.descriptionText(),
                    // Null country — IngestServiceImpl falls back to the admin's ISO-2.
                    null,
                    firstNonBlank(li.citiesDerived()),
                    remote,
                    li.url(),
                    li.datePosted(),
                    null,
                    null,
                    null,
                    null));
        }
        return out;
    }

    private static String firstNonBlank(List<String> values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static Optional<String> optional(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static void sleepBackoff(int attempt) {
        long sleep = BASE_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static class LinkedInRateLimitedException extends RuntimeException {
        public LinkedInRateLimitedException(String message) {
            super(message);
        }
    }

    public static class LinkedInUnavailableException extends RuntimeException {
        public LinkedInUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
