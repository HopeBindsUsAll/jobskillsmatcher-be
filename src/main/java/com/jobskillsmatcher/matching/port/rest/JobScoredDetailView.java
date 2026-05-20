package com.jobskillsmatcher.matching.port.rest;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.model.Seniority;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobScoredDetailView(
        UUID id,
        String externalId,
        String title,
        String company,
        String country,
        String city,
        boolean remote,
        Seniority seniority,
        OffsetDateTime postedAt,
        String sourceUrl,
        String description,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String currency,
        String salaryPeriod,
        ScoreBreakdown breakdown
) {
    public static JobScoredDetailView from(Job e, ScoreBreakdown breakdown) {
        return new JobScoredDetailView(
                e.getId(),
                e.getExternalId(),
                e.getTitle(),
                e.getCompany(),
                e.getCountry(),
                e.getCity(),
                e.isRemote(),
                e.getSeniority(),
                e.getPostedAt(),
                e.getSourceUrl(),
                e.getDescription(),
                e.getMinSalary(),
                e.getMaxSalary(),
                e.getCurrency(),
                e.getSalaryPeriod(),
                breakdown);
    }
}
