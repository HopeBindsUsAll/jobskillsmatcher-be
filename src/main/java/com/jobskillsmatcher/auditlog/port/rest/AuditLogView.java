package com.jobskillsmatcher.auditlog.port.rest;

import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.LogAction;
import com.jobskillsmatcher.auditlog.model.LogCategory;
import com.jobskillsmatcher.auditlog.model.LogLevel;
import com.jobskillsmatcher.auditlog.model.LogOutcome;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogView(
        UUID id,
        OffsetDateTime createdAt,
        LogCategory category,
        LogLevel level,
        LogOutcome outcome,
        UUID actorUserId,
        String actorEmail,
        String actorRole,
        LogAction action,
        String httpMethod,
        String path,
        Integer statusCode,
        String ipAddress,
        String userAgent,
        Long durationMs,
        String requestId,
        String message,
        String loggerName
) {

    public static AuditLogView from(AuditLog e, String resolvedEmail) {
        return new AuditLogView(
                e.getId(),
                e.getCreatedAt(),
                e.getCategory(),
                e.getLevel(),
                e.getOutcome(),
                e.getActorUserId(),
                e.getActorEmail() != null ? e.getActorEmail() : resolvedEmail,
                e.getActorRole(),
                e.getAction(),
                e.getHttpMethod(),
                e.getPath(),
                e.getStatusCode(),
                e.getIpAddress(),
                e.getUserAgent(),
                e.getDurationMs(),
                e.getRequestId(),
                e.getMessage(),
                e.getLoggerName());
    }
}
