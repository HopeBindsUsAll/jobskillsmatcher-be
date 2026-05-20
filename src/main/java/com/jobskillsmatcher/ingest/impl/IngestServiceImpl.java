package com.jobskillsmatcher.ingest.impl;

import com.jobskillsmatcher.ingest.IngestService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.context.cache.CacheConfig;
import com.jobskillsmatcher.ingest.impl.client.JSearchClient;
import com.jobskillsmatcher.ingest.impl.client.JSearchJob;
import com.jobskillsmatcher.ingest.impl.client.LinkedInJobClient;
import com.jobskillsmatcher.ingest.impl.groq.GroqSkillEnricher;
import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;
import com.jobskillsmatcher.ingest.IngestRunRepository;
import com.jobskillsmatcher.ingest.impl.jpa.IngestSchedule;
import com.jobskillsmatcher.ingest.IngestScheduleRepository;
import com.jobskillsmatcher.ingest.model.IngestRequest;
import com.jobskillsmatcher.ingest.model.RunStatus;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.impl.jpa.JobSkillId;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.job.model.Seniority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestServiceImpl implements IngestService {

    private final JSearchClient jsearchClient;
    private final LinkedInJobClient linkedInJobClient;
    private final JobSkillExtractor jobSkillExtractor;
    private final GroqSkillEnricher groqSkillEnricher;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final IngestRunRepository ingestRunRepository;
    private final IngestScheduleRepository ingestScheduleRepository;
    private final ObjectMapper objectMapper;

    // Each job is stored in its own transaction so one bad payload doesn't fail the batch.
    // Evict the feed cache once per run, not per job.
    @CacheEvict(cacheNames = CacheConfig.JOB_FEED, allEntries = true)
    public IngestRun runOnce(IngestRequest request) {
        IngestRun run = new IngestRun();
        run.setId(UUID.randomUUID());
        run.setScheduleId(request.scheduleId());
        run.setQuery(request.query());
        run.setCountry(request.country() == null ? "" : request.country());
        run.setStartedAt(OffsetDateTime.now());
        run.setStatus(RunStatus.RUNNING);
        ingestRunRepository.save(run);

        try {
            List<JSearchJob> jobs;
            if (request.useLinkedin()) {
                // LinkedIn wants the full country name, not the ISO-2 code.
                jobs = linkedInJobClient.search(
                        request.query(),
                        countryFullName(request.country()));
            } else {
                jobs = jsearchClient.search(
                        request.query(),
                        request.country(),
                        Math.max(request.page(), 1),
                        Math.max(request.numPages(), 1));
            }
            run.setFetchedCount(jobs.size());

            int stored = 0;
            for (JSearchJob raw : jobs) {
                if (raw.jobId() == null || raw.jobId().isBlank()) {
                    continue;
                }
                try {
                    if (storeJob(raw, request)) {
                        stored++;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to store job {}: {}", raw.jobId(), ex.getMessage());
                }
            }
            run.setStoredCount(stored);
            run.setStatus(RunStatus.SUCCESS);

            if (request.scheduleId() != null) {
                ingestScheduleRepository.findById(request.scheduleId()).ifPresent(s -> {
                    s.setLastRunAt(OffsetDateTime.now());
                    ingestScheduleRepository.save(s);
                });
            }
        } catch (Exception ex) {
            run.setStatus(RunStatus.FAILED);
            run.setError(truncate(ex.getMessage(), 4000));
            log.error("Ingest run {} failed", run.getId(), ex);
        } finally {
            run.setFinishedAt(OffsetDateTime.now());
            ingestRunRepository.save(run);
        }
        return run;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean storeJob(JSearchJob raw, IngestRequest request) {
        Optional<Job> existing = jobRepository.findByExternalId(raw.jobId());
        Job job = existing.orElseGet(() -> {
            Job j = new Job();
            j.setId(UUID.randomUUID());
            j.setExternalId(raw.jobId());
            return j;
        });

        job.setTitle(blankIfNull(raw.title()));
        job.setCompany(blankIfNull(raw.employerName()));
        job.setCountry(normaliseCountry(raw.country(), request.country()));
        job.setCity(blankIfNull(raw.city()));
        job.setRemote(Boolean.TRUE.equals(raw.remote()) || request.remote());
        job.setDescription(blankIfNull(raw.description()));
        job.setSourceUrl(blankIfNull(raw.applyLink()));
        job.setPostedAt(raw.postedAt());
        job.setSeniority(guessSeniority(raw.title()));
        job.setMinSalary(raw.minSalary());
        job.setMaxSalary(raw.maxSalary());
        job.setCurrency(normaliseCurrency(raw.salaryCurrency()));
        job.setSalaryPeriod(blankIfNull(raw.salaryPeriod()).toUpperCase(java.util.Locale.ROOT));
        job.setRawPayload(toJson(raw));
        jobRepository.save(job);

        replaceJobSkills(job);
        return existing.isEmpty();
    }

    private void replaceJobSkills(Job job) {
        jobSkillRepository.deleteByIdJobId(job.getId());
        Map<String, Requirement> algo = jobSkillExtractor.extract(job.getDescription());
        Map<String, Requirement> extracted = groqSkillEnricher.enrich(job.getDescription(), algo);
        for (Map.Entry<String, Requirement> entry : extracted.entrySet()) {
            JobSkill link = new JobSkill();
            link.setId(new JobSkillId(job.getId(), entry.getKey()));
            link.setRequirement(entry.getValue());
            jobSkillRepository.save(link);
        }
    }

    public IngestRun runForSchedule(IngestSchedule schedule) {
        IngestRequest request = IngestRequest.forSchedule(
                schedule.getId(),
                schedule.getQuery(),
                schedule.getCountry(),
                schedule.getCity(),
                schedule.isRemote(),
                schedule.isUseLinkedin());
        return runOnce(request);
    }

    // ISO-2 -> full English name for LinkedIn's location_filter. Returns "" when unknown.
    private static String countryFullName(String iso2) {
        if (iso2 == null || iso2.isBlank()) {
            return "";
        }
        String code = iso2.trim().toUpperCase(Locale.ROOT);
        String name = Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
        // getDisplayCountry echoes the code back when the region is unknown.
        return name == null || name.equalsIgnoreCase(code) ? "" : name;
    }

    private String toJson(JSearchJob raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise raw payload for {}: {}", raw.jobId(), ex.getMessage());
            return null;
        }
    }

    private static String blankIfNull(String s) {
        return s == null ? "" : s;
    }

    private static String normaliseCurrency(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String upper = raw.trim().toUpperCase(java.util.Locale.ROOT);
        return upper.length() <= 3 ? upper : upper.substring(0, 3);
    }

    private static String normaliseCountry(String raw, String fallback) {
        String value = raw == null || raw.isBlank() ? fallback : raw;
        if (value == null || value.isBlank()) {
            return "";
        }
        String upper = value.toUpperCase(java.util.Locale.ROOT);
        return upper.length() <= 2 ? upper : upper.substring(0, 2);
    }

    private static Seniority guessSeniority(String title) {
        if (title == null) return Seniority.UNKNOWN;
        String lower = title.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("intern")) return Seniority.INTERN;
        if (lower.contains("junior") || lower.contains("entry")) return Seniority.JUNIOR;
        if (lower.contains("lead") || lower.contains("principal") || lower.contains("staff")) return Seniority.LEAD;
        if (lower.contains("senior") || lower.contains("sr.")) return Seniority.SENIOR;
        if (lower.contains("mid")) return Seniority.MID;
        return Seniority.UNKNOWN;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
