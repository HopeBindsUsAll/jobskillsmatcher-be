package com.jobskillsmatcher.auditlog.impl;

import com.jobskillsmatcher.auditlog.AuditLogRepository;
import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(AuditLog row) {
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAll(List<AuditLog> rows) {
        repository.saveAll(rows);
    }
}
