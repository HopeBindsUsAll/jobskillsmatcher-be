package com.jobskillsmatcher.cv.port.rest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CvScanResultView(
        UUID uploadId,
        String filename,
        long sizeBytes,
        String contentType,
        OffsetDateTime uploadedAt,
        List<CvSkillView> extracted,
        List<CvSkillView> missingForPreferredRole,
        String preferredRole
) { }
