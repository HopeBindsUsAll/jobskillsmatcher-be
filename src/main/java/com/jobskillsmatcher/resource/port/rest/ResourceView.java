package com.jobskillsmatcher.resource.port.rest;

import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.model.ResourceDifficulty;
import com.jobskillsmatcher.resource.model.ResourceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResourceView(
        UUID id,
        ResourceType type,
        ResourceDifficulty difficulty,
        String title,
        String description,
        String url,
        String provider,
        List<ResourceSkillRef> skills,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ResourceView from(LearningResource e, List<ResourceSkillRef> skills) {
        return new ResourceView(
                e.getId(),
                e.getType(),
                e.getDifficulty(),
                e.getTitle(),
                e.getDescription(),
                e.getUrl(),
                e.getProvider(),
                skills,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
