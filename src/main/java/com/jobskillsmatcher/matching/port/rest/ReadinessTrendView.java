package com.jobskillsmatcher.matching.port.rest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReadinessTrendView(
        List<Point> points
) {
    public record Point(
            UUID snapshotId,
            OffsetDateTime capturedAt,
            double score,
            List<UUID> topJobIds
    ) { }
}
