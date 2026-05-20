package com.jobskillsmatcher.ingest.impl.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JSearchJob(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("job_title") String title,
        @JsonProperty("employer_name") String employerName,
        @JsonProperty("job_description") String description,
        @JsonProperty("job_country") String country,
        @JsonProperty("job_city") String city,
        @JsonProperty("job_is_remote") Boolean remote,
        @JsonProperty("job_apply_link") String applyLink,
        @JsonProperty("job_posted_at_datetime_utc") OffsetDateTime postedAt,
        @JsonProperty("job_min_salary") java.math.BigDecimal minSalary,
        @JsonProperty("job_max_salary") java.math.BigDecimal maxSalary,
        @JsonProperty("job_salary_currency") String salaryCurrency,
        @JsonProperty("job_salary_period") String salaryPeriod
) { }
