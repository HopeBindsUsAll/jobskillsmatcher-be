package com.jobskillsmatcher.auditlog;

import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.AuditLogEntry;
import com.jobskillsmatcher.auditlog.model.AuditLogFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void record(AuditLogEntry entry);

    Page<AuditLog> search(AuditLogFilter filter, Pageable pageable);
}
