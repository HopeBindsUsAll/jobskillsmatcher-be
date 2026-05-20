package com.jobskillsmatcher.auditlog.impl.jpa;

import com.jobskillsmatcher.auditlog.model.LogAction;
import com.jobskillsmatcher.auditlog.model.LogCategory;
import com.jobskillsmatcher.auditlog.model.LogLevel;
import com.jobskillsmatcher.auditlog.model.LogOutcome;
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
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LogCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LogLevel level;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LogOutcome outcome;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LogAction action;

    @Column(name = "http_method", length = 8)
    private String httpMethod;

    @Column(length = 512)
    private String path;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "logger_name", length = 255)
    private String loggerName;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (level == null) {
            level = LogLevel.INFO;
        }
    }
}
