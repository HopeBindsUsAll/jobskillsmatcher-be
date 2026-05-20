package com.jobskillsmatcher.ingest.port.rest;

import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleView(
        UUID id,
        String name,
        String query,
        String country,
        String city,
        boolean remote,
        boolean useLinkedin,
        String cronExpression,
        boolean enabled,
        OffsetDateTime lastRunAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ScheduleView from(IngestSchedule e) {
        return new ScheduleView(
                e.getId(),
                e.getName(),
                e.getQuery(),
                e.getCountry(),
                e.getCity(),
                e.isRemote(),
                e.isUseLinkedin(),
                e.getCronExpression(),
                e.isEnabled(),
                e.getLastRunAt(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
