package com.jobskillsmatcher.auditlog.impl;

import com.jobskillsmatcher.auditlog.AuditLogRepository;
import com.jobskillsmatcher.auditlog.model.LogCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

// Daily log purge. AUDIT rows are PII-bearing so we keep them longer than SYSTEM noise.
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRetentionJob {

    private final AuditLogRepository repository;

    @Value("${jobskillsmatcher.auditlog.retention-days.audit:180}")
    private int auditRetentionDays;

    @Value("${jobskillsmatcher.auditlog.retention-days.system:30}")
    private int systemRetentionDays;

    @Scheduled(cron = "${jobskillsmatcher.auditlog.retention-cron:0 30 3 * * *}")
    @Transactional
    public void purge() {
        OffsetDateTime now = OffsetDateTime.now();
        int audit = repository.deleteByCategoryOlderThan(
                LogCategory.AUDIT, now.minusDays(auditRetentionDays));
        int system = repository.deleteByCategoryOlderThan(
                LogCategory.SYSTEM, now.minusDays(systemRetentionDays));
        if (audit + system > 0) {
            log.info("Audit-log retention purge removed {} audit and {} system rows",
                    audit, system);
        }
    }
}
