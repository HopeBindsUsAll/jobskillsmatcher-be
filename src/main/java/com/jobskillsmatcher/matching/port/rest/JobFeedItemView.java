package com.jobskillsmatcher.matching.port.rest;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.model.Seniority;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobFeedItemView(
        UUID id,
        String title,
        String company,
        String country,
        String city,
        boolean remote,
        Seniority seniority,
        OffsetDateTime postedAt,
        String sourceUrl,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String currency,
        String salaryPeriod,
        double score,
        double jaccard,
        double cosine,
        int matchedCount,
        int missingRequiredCount,
        int missingPreferredCount
) {
    public static JobFeedItemView from(Job e,
                                       double score,
                                       double jaccard,
                                       double cosine,
                                       int matched,
                                       int missingRequired,
                                       int missingPreferred) {
        return new JobFeedItemView(
                e.getId(),
                e.getTitle(),
                e.getCompany(),
                e.getCountry(),
                e.getCity(),
                e.isRemote(),
                e.getSeniority(),
                e.getPostedAt(),
                e.getSourceUrl(),
                e.getMinSalary(),
                e.getMaxSalary(),
                e.getCurrency(),
                e.getSalaryPeriod(),
                score,
                jaccard,
                cosine,
                matched,
                missingRequired,
                missingPreferred);
    }
}
