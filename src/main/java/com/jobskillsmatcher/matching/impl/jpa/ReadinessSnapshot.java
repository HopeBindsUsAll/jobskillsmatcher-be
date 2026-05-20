package com.jobskillsmatcher.matching.impl.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "readiness_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class ReadinessSnapshot {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(nullable = false)
    private double score;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "top_job_ids", nullable = false, columnDefinition = "uuid[]")
    private UUID[] topJobIds = new UUID[0];

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (capturedAt == null) {
            capturedAt = createdAt;
        }
        if (topJobIds == null) {
            topJobIds = new UUID[0];
        }
    }
}
