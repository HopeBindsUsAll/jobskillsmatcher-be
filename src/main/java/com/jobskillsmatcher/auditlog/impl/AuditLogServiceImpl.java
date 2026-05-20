package com.jobskillsmatcher.auditlog.impl;

import com.jobskillsmatcher.auditlog.AuditLogRepository;
import com.jobskillsmatcher.auditlog.AuditLogService;
import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.AuditLogEntry;
import com.jobskillsmatcher.auditlog.model.AuditLogFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MESSAGE_MAX = 8000;
    private static final int UA_MAX = 256;

    private final AuditLogRepository repository;
    private final AuditLogWriter writer;

    @Override
    public void record(AuditLogEntry e) {
        try {
            AuditLog row = new AuditLog();
            row.setCategory(e.category());
            row.setLevel(e.level());
            row.setOutcome(e.outcome());
            row.setAction(e.action());
            row.setActorUserId(e.actorUserId());
            row.setActorEmail(e.actorEmail());
            row.setActorRole(e.actorRole());
            row.setHttpMethod(e.httpMethod());
            row.setPath(e.path());
            row.setStatusCode(e.statusCode());
            row.setIpAddress(e.ipAddress());
            row.setUserAgent(truncate(e.userAgent(), UA_MAX));
            row.setDurationMs(e.durationMs());
            row.setRequestId(e.requestId());
            row.setMessage(truncate(e.message(), MESSAGE_MAX));
            row.setLoggerName(e.loggerName());
            writer.persist(row);
        } catch (RuntimeException ex) {
            log.debug("Failed to persist audit log row", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditLogFilter filter, Pageable pageable) {
        return repository.findAll(AuditLogSpecifications.matching(filter), pageable);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
