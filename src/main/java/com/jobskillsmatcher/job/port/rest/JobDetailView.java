package com.jobskillsmatcher.job.port.rest;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.model.Seniority;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JobDetailView(
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
        List<JobSkillView> requiredSkills,
        List<JobSkillView> preferredSkills
) {
    public static JobDetailView from(Job e, List<JobSkillView> required, List<JobSkillView> preferred) {
        return new JobDetailView(
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
                required,
                preferred);
    }
}
