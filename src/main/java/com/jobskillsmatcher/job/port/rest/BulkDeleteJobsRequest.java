package com.jobskillsmatcher.job.port.rest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkDeleteJobsRequest(
        @NotEmpty
        @Size(max = 500, message = "At most 500 jobs can be deleted at once")
        List<UUID> ids
) { }
