package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.matching.JobFeedService;
import com.jobskillsmatcher.matching.impl.jpa.ReadinessSnapshot;
import com.jobskillsmatcher.matching.ReadinessSnapshotRepository;
import com.jobskillsmatcher.matching.port.rest.ReadinessHeadlineView;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Captures one readiness snapshot per student each night for the dashboard chart.
@Component
@RequiredArgsConstructor
@Slf4j
public class ReadinessSnapshotScheduler {

    private static final int SNAPSHOT_TOP_N = 10;

    private final StudentProfileRepository studentProfileRepository;
    private final JobFeedService jobFeedService;
    private final ReadinessSnapshotRepository snapshotRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void captureSnapshots() {
        log.info("Starting nightly readiness snapshot");
        int written = 0;
        for (StudentProfile profile : studentProfileRepository.findAll()) {
            try {
                if (writeOne(profile.getUserId())) {
                    written++;
                }
            } catch (RuntimeException ex) {
                log.warn("Snapshot failed for student {}: {}", profile.getUserId(), ex.getMessage());
            }
        }
        log.info("Readiness snapshot done — wrote {} rows", written);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean writeOne(UUID studentId) {
        ReadinessHeadlineView headline = jobFeedService.headline(studentId);
        if (headline.sampleSize() == 0) {
            return false;
        }
        List<UUID> topIds = headline.topJobs().stream()
                .map(ReadinessHeadlineView.TopJob::id)
                .limit(SNAPSHOT_TOP_N)
                .toList();
        ReadinessSnapshot row = new ReadinessSnapshot();
        row.setStudentId(studentId);
        row.setCapturedAt(OffsetDateTime.now());
        row.setScore(headline.score());
        row.setTopJobIds(topIds.toArray(new UUID[0]));
        snapshotRepository.save(row);
        return true;
    }
}
