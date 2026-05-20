package com.jobskillsmatcher.ingest.model;

import java.util.UUID;

public record IngestRequest(
        UUID scheduleId,
        String query,
        String country,
        String city,
        boolean remote,
        boolean useLinkedin,
        int page,
        int numPages
) {
    public static IngestRequest manual(String query, String country, String city,
                                       boolean remote, boolean useLinkedin) {
        return new IngestRequest(null, query, country, city, remote, useLinkedin, 1, 1);
    }

    public static IngestRequest forSchedule(UUID scheduleId, String query, String country,
                                            String city, boolean remote, boolean useLinkedin) {
        return new IngestRequest(scheduleId, query, country, city, remote, useLinkedin, 1, 1);
    }
}
