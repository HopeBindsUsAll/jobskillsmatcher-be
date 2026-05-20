package com.jobskillsmatcher.auditlog.model;

import java.time.OffsetDateTime;


public record AuditLogFilter(
        String email,
        String role,
        LogCategory category,
        LogLevel level,
        LogOutcome outcome,
        LogAction action,
        OffsetDateTime from,
        OffsetDateTime to
) { }
