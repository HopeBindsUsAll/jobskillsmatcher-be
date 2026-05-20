package com.jobskillsmatcher.ingest.port.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertScheduleRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 500) String query,
        @Size(max = 2) String country,
        @Size(max = 255) String city,
        boolean remote,
        boolean useLinkedin,
        @NotBlank @Size(max = 120) String cronExpression,
        Boolean enabled
) { }
