package com.jobskillsmatcher.job.port.rest;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.model.Seniority;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobSummaryView(
        UUID id,
        String externalId,
        String title,
        String company,
        String country,
        String city,
        boolean remote,
        Seniority seniority,
        OffsetDateTime postedAt,
        String sourceUrl
) {
    public static JobSummaryView from(Job e) {
        return new JobSummaryView(
                e.getId(),
                e.getExternalId(),
                e.getTitle(),
                e.getCompany(),
                e.getCountry(),
                e.getCity(),
                e.isRemote(),
                e.getSeniority(),
                e.getPostedAt(),
                e.getSourceUrl());
    }
}
