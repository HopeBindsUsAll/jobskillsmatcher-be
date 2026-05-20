package com.jobskillsmatcher.matching.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.job.model.Seniority;
import com.jobskillsmatcher.matching.JobFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Score-ranked job feed and per-job match breakdown.")
public class JobController {

    private final JobFeedService jobFeedService;

    @GetMapping
    @Operation(summary = "Ranked job feed",
            description = "Paginated job listings ranked by readiness score for the signed-in student.")
    public Page<JobFeedItemView> feed(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(name = "remoteOnly", defaultValue = "false") boolean remoteOnly,
            @RequestParam(required = false) Seniority seniority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobFeedService.feed(
                CurrentUser.requireId(), country, city, remoteOnly, seniority, search, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Job detail with breakdown",
            description = "Returns the job plus matched / missing-required / missing-preferred skill panels and the score breakdown.")
    public JobScoredDetailView detail(@PathVariable UUID id) {
        return jobFeedService.detail(CurrentUser.requireId(), id);
    }
}
