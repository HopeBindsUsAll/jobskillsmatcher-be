package com.jobskillsmatcher.ingest.impl;

import com.jobskillsmatcher.ingest.IngestService;
import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.IngestScheduleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;


@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicSchedulerService {

    private final TaskScheduler taskScheduler;
    private final IngestScheduleRepository ingestScheduleRepository;
    private final IngestService ingestService;

    private final Map<UUID, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    @PostConstruct
    void loadEnabled() {
        for (IngestSchedule schedule : ingestScheduleRepository.findAllByEnabledTrue()) {
            try {
                register(schedule);
            } catch (Exception ex) {
                log.warn("Failed to register schedule {} ({}): {}",
                        schedule.getId(), schedule.getCronExpression(), ex.getMessage());
            }
        }
    }

    @PreDestroy
    void shutdown() {
        for (ScheduledFuture<?> future : futures.values()) {
            future.cancel(false);
        }
        futures.clear();
    }

    public synchronized void register(IngestSchedule schedule) {
        cancel(schedule.getId());
        if (!schedule.isEnabled()) {
            return;
        }
        CronTrigger trigger = new CronTrigger(schedule.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(() -> safeRun(schedule.getId()), trigger);
        if (future != null) {
            futures.put(schedule.getId(), future);
        }
        log.info("Scheduled ingest {} with cron '{}'", schedule.getId(), schedule.getCronExpression());
    }

    public synchronized void cancel(UUID scheduleId) {
        ScheduledFuture<?> future = futures.remove(scheduleId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled scheduled ingest {}", scheduleId);
        }
    }

    public boolean hasFuture(UUID scheduleId) {
        ScheduledFuture<?> future = futures.get(scheduleId);
        return future != null && !future.isCancelled();
    }

    private void safeRun(UUID scheduleId) {
        ingestScheduleRepository.findById(scheduleId).ifPresent(schedule -> {
            if (!schedule.isEnabled()) {
                return;
            }
            try {
                ingestService.runForSchedule(schedule);
            } catch (Exception ex) {
                log.error("Scheduled ingest {} failed", scheduleId, ex);
            }
        });
    }
}
