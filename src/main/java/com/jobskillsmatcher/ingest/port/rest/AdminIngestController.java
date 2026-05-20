package com.jobskillsmatcher.ingest.port.rest;

import com.jobskillsmatcher.ingest.IngestService;
import com.jobskillsmatcher.ingest.ScheduleService;
import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;
import com.jobskillsmatcher.ingest.IngestRunRepository;
import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.model.IngestRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/ingest")
@RequiredArgsConstructor
public class AdminIngestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final IngestService ingestService;
    private final ScheduleService scheduleService;
    private final IngestRunRepository ingestRunRepository;

    @PostMapping("/run")
    public IngestRunView runOnce(@Valid @RequestBody RunIngestRequest req) {
        IngestRun run = ingestService.runOnce(IngestRequest.manual(
                req.query(),
                req.country() == null ? "" : req.country().toUpperCase(java.util.Locale.ROOT),
                req.city() == null ? "" : req.city(),
                req.remote(),
                req.useLinkedin()));
        return IngestRunView.from(run);
    }

    @GetMapping("/schedules")
    public List<ScheduleView> listSchedules() {
        return scheduleService.listAll().stream().map(ScheduleView::from).toList();
    }

    @PostMapping("/schedules")
    public ScheduleView createSchedule(@Valid @RequestBody UpsertScheduleRequest req) {
        return ScheduleView.from(scheduleService.create(req));
    }

    @PutMapping("/schedules/{id}")
    public ScheduleView updateSchedule(@PathVariable UUID id, @Valid @RequestBody UpsertScheduleRequest req) {
        return ScheduleView.from(scheduleService.update(id, req));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/schedules/{id}/run")
    public IngestRunView runSchedule(@PathVariable UUID id) {
        IngestSchedule schedule = scheduleService.get(id);
        IngestRun run = ingestService.runForSchedule(schedule);
        return IngestRunView.from(run);
    }

    @GetMapping("/runs")
    public Page<IngestRunView> listRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return ingestRunRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(Math.max(page, 0), safeSize))
                .map(IngestRunView::from);
    }
}
