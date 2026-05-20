package com.jobskillsmatcher.job.impl.jpa;

import com.jobskillsmatcher.job.model.Seniority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job")
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 255, unique = true)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String company;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, length = 255)
    private String city;

    @Column(nullable = false)
    private boolean remote;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Seniority seniority = Seniority.UNKNOWN;

    @Column(nullable = false)
    private String description;

    @Column(name = "source_url", nullable = false, length = 2000)
    private String sourceUrl;

    @Column(name = "posted_at")
    private OffsetDateTime postedAt;

    @Column(name = "min_salary", precision = 12, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;

    @Column(length = 3)
    private String currency;

    @Column(name = "salary_period", length = 16)
    private String salaryPeriod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (company == null) company = "";
        if (country == null) country = "";
        if (city == null) city = "";
        if (description == null) description = "";
        if (sourceUrl == null) sourceUrl = "";
        if (seniority == null) seniority = Seniority.UNKNOWN;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
