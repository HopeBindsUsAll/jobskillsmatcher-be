package com.jobskillsmatcher.matching.port.rest;

import java.util.List;

public record ReadinessHeadlineView(
        double score,
        int sampleSize,
        String country,
        String preferredRole,
        List<TopJob> topJobs
) {
    public record TopJob(java.util.UUID id, String title, String company, double score) { }
}
