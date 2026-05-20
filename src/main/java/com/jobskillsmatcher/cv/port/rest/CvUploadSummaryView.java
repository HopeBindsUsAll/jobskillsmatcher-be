package com.jobskillsmatcher.cv.port.rest;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CvUploadSummaryView(
        UUID uploadId,
        String filename,
        long sizeBytes,
        String contentType,
        OffsetDateTime uploadedAt,
        int extractedSkillCount
) { }
