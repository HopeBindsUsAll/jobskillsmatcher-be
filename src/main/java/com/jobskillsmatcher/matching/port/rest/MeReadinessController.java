package com.jobskillsmatcher.matching.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.matching.JobFeedService;
import com.jobskillsmatcher.matching.impl.ReadinessSnapshotScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Readiness", description = "Job-fit headline + historical trend for the signed-in student.")
public class MeReadinessController {

    private final JobFeedService jobFeedService;
    private final ReadinessSnapshotScheduler snapshotScheduler;

    @GetMapping("/readiness")
    @Operation(summary = "Headline readiness",
            description = "Average final-score across the top-10 jobs that match preferred role + country.")
    public ReadinessHeadlineView readiness() {
        return jobFeedService.headline(CurrentUser.requireId());
    }

    @GetMapping("/readiness/trend")
    @Operation(summary = "Readiness trend",
            description = "Last 30 nightly snapshots in chronological order.")
    public ReadinessTrendView trend() {
        return jobFeedService.trend(CurrentUser.requireId());
    }

    @PostMapping("/readiness/trend/refresh")
    @Operation(summary = "Capture an on-demand readiness snapshot",
            description = "Writes a fresh ReadinessSnapshot row for the signed-in student and returns the updated trend.")
    public ReadinessTrendView refreshTrend() {
        java.util.UUID studentId = CurrentUser.requireId();
        snapshotScheduler.writeOne(studentId);
        return jobFeedService.trend(studentId);
    }
}
