package com.jobskillsmatcher.auditlog.model;

import java.util.UUID;


public record AuditLogEntry(
        LogCategory category,
        LogLevel level,
        LogOutcome outcome,
        LogAction action,
        UUID actorUserId,
        String actorEmail,
        String actorRole,
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

    //A user-action audit event (security events: login/logout/register/refresh).
    public static AuditLogEntry audit(LogAction action, LogLevel level, LogOutcome outcome,
                                      UUID actorUserId, String actorEmail, String actorRole,
                                      String message) {
        return new AuditLogEntry(LogCategory.AUDIT, level, outcome, action,
                actorUserId, actorEmail, actorRole,
                null, null, null, null, null, null, null, message, null);
    }

    // A mutating HTTP request audit event.
    public static AuditLogEntry httpMutation(LogLevel level, LogOutcome outcome,
                                             UUID actorUserId, String actorRole,
                                             String httpMethod, String path, Integer statusCode,
                                             String ipAddress, String userAgent,
                                             Long durationMs, String requestId, String message) {
        return new AuditLogEntry(LogCategory.AUDIT, level, outcome, LogAction.HTTP_MUTATION,
                actorUserId, null, actorRole,
                httpMethod, path, statusCode, ipAddress, userAgent,
                durationMs, requestId, message, null);
    }

    // A persisted application WARN/ERROR log event.
    public static AuditLogEntry system(LogLevel level, String loggerName, String message) {
        return new AuditLogEntry(LogCategory.SYSTEM, level, null, LogAction.SYSTEM_EVENT,
                null, null, null, null, null, null, null, null, null, null,
                message, loggerName);
    }
}
