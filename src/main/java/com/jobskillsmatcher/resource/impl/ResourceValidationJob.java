package com.jobskillsmatcher.resource.impl;

import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.LearningResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

// Daily URL liveness check. Dead links are flagged so recommendations skip them.
// Runs at 02:30 to stay clear of the 03:00 readiness snapshot.
@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceValidationJob {

    private static final int BATCH_SIZE = 50;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final long INTER_REQUEST_SLEEP_MS = 1000L;

    private final LearningResourceRepository resourceRepository;
    private final RestClient validationRestClient;

    @Scheduled(cron = "0 30 2 * * *")
    public void validateAll() {
        log.info("Starting learning-resource URL validation pass");
        List<LearningResource> all = resourceRepository.findAll();
        int alive = 0;
        int dead = 0;
        for (int i = 0; i < all.size(); i += BATCH_SIZE) {
            List<LearningResource> batch = all.subList(i, Math.min(i + BATCH_SIZE, all.size()));
            for (LearningResource resource : batch) {
                boolean ok = checkUrl(resource.getUrl());
                applyResult(resource.getId(), ok);
                if (ok) alive++; else dead++;
                sleepBetweenRequests();
            }
        }
        log.info("Resource validation finished: {} alive, {} dead", alive, dead);
    }

    @Transactional
    public void applyResult(java.util.UUID resourceId, boolean alive) {
        resourceRepository.findById(resourceId).ifPresent(resource -> {
            resource.setUrlAlive(alive);
            resource.setLastValidatedAt(OffsetDateTime.now());
            resourceRepository.save(resource);
        });
    }

    private boolean checkUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            HttpStatusCode status = validationRestClient.head()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            return status.is2xxSuccessful() || status.is3xxRedirection();
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            // Some sites refuse HEAD with 405 but answer GET fine.
            if (status.value() == 405) {
                return checkUrlWithGet(url);
            }
            return status.is3xxRedirection();
        } catch (Exception ex) {
            log.debug("HEAD {} failed: {}", url, ex.getMessage());
            return false;
        }
    }

    private boolean checkUrlWithGet(String url) {
        try {
            HttpStatusCode status = validationRestClient.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            return status.is2xxSuccessful() || status.is3xxRedirection();
        } catch (Exception ex) {
            return false;
        }
    }

    private static void sleepBetweenRequests() {
        try {
            Thread.sleep(INTER_REQUEST_SLEEP_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    static Duration httpTimeout() {
        return HTTP_TIMEOUT;
    }
}
