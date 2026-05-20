package com.jobskillsmatcher.resource.port.rest;

import com.jobskillsmatcher.resource.model.ResourceDifficulty;
import com.jobskillsmatcher.resource.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertResourceRequest(
        @NotNull ResourceType type,
        @NotNull ResourceDifficulty difficulty,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 2000) String url,
        @Size(max = 200) String provider,
        @NotNull List<@NotBlank @Size(max = 255) String> skillIds
) { }
