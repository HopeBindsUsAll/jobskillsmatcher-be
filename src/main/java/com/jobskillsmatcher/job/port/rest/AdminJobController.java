package com.jobskillsmatcher.job.port.rest;

import com.jobskillsmatcher.job.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
public class AdminJobController {

    private final JobService jobService;

    @GetMapping
    public Page<JobSummaryView> list(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(name = "remote", defaultValue = "false") boolean remoteOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.list(country, city, remoteOnly, page, size);
    }

    @GetMapping("/{id}")
    public JobDetailView get(@PathVariable UUID id) {
        return jobService.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    public BulkDeleteJobsResult bulkDelete(@Valid @RequestBody BulkDeleteJobsRequest req) {
        return new BulkDeleteJobsResult(jobService.deleteAll(req.ids()));
    }
}
