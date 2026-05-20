package com.jobskillsmatcher.ingest.port.rest;

import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;
import com.jobskillsmatcher.ingest.model.RunStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngestRunView(
        UUID id,
        UUID scheduleId,
        String query,
        String country,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        RunStatus status,
        int fetchedCount,
        int storedCount,
        String error
) {
    public static IngestRunView from(IngestRun e) {
        return new IngestRunView(
                e.getId(),
                e.getScheduleId(),
                e.getQuery(),
                e.getCountry(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getStatus(),
                e.getFetchedCount(),
                e.getStoredCount(),
                e.getError());
    }
}
