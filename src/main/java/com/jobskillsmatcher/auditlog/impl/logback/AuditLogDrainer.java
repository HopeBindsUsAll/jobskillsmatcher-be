package com.jobskillsmatcher.auditlog.impl.logback;

import com.jobskillsmatcher.auditlog.impl.AuditLogWriter;
import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.LogAction;
import com.jobskillsmatcher.auditlog.model.LogCategory;
import com.jobskillsmatcher.auditlog.model.LogLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogDrainer {

    private static final int BATCH = 500;

    private final AuditLogWriter writer;

    @Scheduled(fixedDelayString = "${jobskillsmatcher.auditlog.drain-ms:5000}")
    public void drain() {
        List<AuditLogQueueAppender.QueuedEvent> events = AuditLogQueueAppender.drain(BATCH);
        if (events.isEmpty()) {
            return;
        }
        List<AuditLog> rows = new ArrayList<>(events.size());
        for (AuditLogQueueAppender.QueuedEvent e : events) {
            AuditLog row = new AuditLog();
            row.setCreatedAt(OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(e.timestamp()), ZoneOffset.UTC));
            row.setCategory(LogCategory.SYSTEM);
            row.setLevel("ERROR".equals(e.level()) ? LogLevel.ERROR : LogLevel.WARN);
            row.setAction(LogAction.SYSTEM_EVENT);
            row.setLoggerName(truncate(e.loggerName(), 255));
            row.setMessage(truncate(e.message(), 8000));
            rows.add(row);
        }

        AuditLogQueueAppender.SUPPRESS.set(Boolean.TRUE);
        try {
            writer.persistAll(rows);
        } catch (RuntimeException ex) {
            log.debug("Dropped {} system-log rows; drain persist failed", rows.size(), ex);
        } finally {
            AuditLogQueueAppender.SUPPRESS.set(Boolean.FALSE);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
