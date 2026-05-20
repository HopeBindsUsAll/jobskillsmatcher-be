package com.jobskillsmatcher.ingest.impl.jpa;

import com.jobskillsmatcher.ingest.model.RunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingest_run")
@Getter
@Setter
@NoArgsConstructor
public class IngestRun {

    @Id
    private UUID id;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "fetched_count", nullable = false)
    private int fetchedCount;

    @Column(name = "stored_count", nullable = false)
    private int storedCount;

    @Column
    private String error;

    @PrePersist
    void prePersist() {
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = RunStatus.RUNNING;
        }
        if (country == null) country = "";
    }
}
