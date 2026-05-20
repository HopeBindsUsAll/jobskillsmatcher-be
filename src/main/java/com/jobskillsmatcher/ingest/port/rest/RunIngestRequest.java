package com.jobskillsmatcher.ingest.port.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RunIngestRequest(
        @NotBlank @Size(max = 500) String query,
        @Size(max = 2) String country,
        @Size(max = 255) String city,
        boolean remote,
        boolean useLinkedin
) { }
