package com.jobskillsmatcher.ingest.impl;

import com.jobskillsmatcher.ingest.ScheduleService;

import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.IngestScheduleRepository;
import com.jobskillsmatcher.ingest.port.rest.UpsertScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final IngestScheduleRepository repository;
    private final DynamicSchedulerService dynamicScheduler;

    @Transactional(readOnly = true)
    public List<IngestSchedule> listAll() {
        return repository.findAll();
    }

    @Transactional
    public IngestSchedule create(UpsertScheduleRequest req) {
        validateCron(req.cronExpression());
        IngestSchedule entity = new IngestSchedule();
        entity.setId(UUID.randomUUID());
        applyRequest(entity, req);
        IngestSchedule saved = repository.save(entity);
        dynamicScheduler.register(saved);
        return saved;
    }

    @Transactional
    public IngestSchedule update(UUID id, UpsertScheduleRequest req) {
        validateCron(req.cronExpression());
        IngestSchedule existing = repository.findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException(id));
        applyRequest(existing, req);
        IngestSchedule saved = repository.save(existing);
        dynamicScheduler.register(saved);
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        IngestSchedule existing = repository.findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException(id));
        dynamicScheduler.cancel(id);
        repository.delete(existing);
    }

    @Transactional(readOnly = true)
    public IngestSchedule get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException(id));
    }

    private void applyRequest(IngestSchedule entity, UpsertScheduleRequest req) {
        entity.setName(req.name().trim());
        entity.setQuery(req.query().trim());
        entity.setCountry(req.country() == null ? "" : req.country().trim().toUpperCase(java.util.Locale.ROOT));
        entity.setCity(req.city() == null ? "" : req.city().trim());
        entity.setRemote(req.remote());
        entity.setUseLinkedin(req.useLinkedin());
        entity.setCronExpression(req.cronExpression().trim());
        entity.setEnabled(req.enabled() == null ? true : req.enabled());
    }

    private static void validateCron(String expression) {
        try {
            CronExpression.parse(expression);
        } catch (IllegalArgumentException ex) {
            throw new InvalidCronException(expression, ex.getMessage());
        }
    }

    public static class ScheduleNotFoundException extends RuntimeException {
        public ScheduleNotFoundException(UUID id) {
            super("Schedule not found: " + id);
        }
    }

    public static class InvalidCronException extends RuntimeException {
        public InvalidCronException(String expression, String reason) {
            super("Invalid cron '" + expression + "': " + reason);
        }
    }
}
