package com.jobskillsmatcher.job.impl;

import com.jobskillsmatcher.job.JobService;

import com.jobskillsmatcher.context.cache.CacheConfig;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.port.rest.JobDetailView;
import com.jobskillsmatcher.job.port.rest.JobSkillView;
import com.jobskillsmatcher.job.port.rest.JobSummaryView;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public Page<JobSummaryView> list(String country, String city, boolean remoteOnly, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return jobRepository.filter(
                        normaliseCountry(country),
                        city == null ? "" : city,
                        remoteOnly,
                        PageRequest.of(Math.max(page, 0), safeSize))
                .map(JobSummaryView::from);
    }

    @Transactional(readOnly = true)
    public JobDetailView get(UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        List<JobSkill> links = jobSkillRepository.findAllByIdJobId(id);
        Map<String, Skill> skillsById = new HashMap<>();
        if (!links.isEmpty()) {
            List<String> ids = links.stream().map(l -> l.getId().getSkillId()).toList();
            for (Skill s : skillRepository.findAllById(ids)) {
                skillsById.put(s.getId(), s);
            }
        }

        List<JobSkillView> required = new ArrayList<>();
        List<JobSkillView> preferred = new ArrayList<>();
        for (JobSkill link : links) {
            Skill skill = skillsById.get(link.getId().getSkillId());
            if (skill == null) continue;
            JobSkillView view = new JobSkillView(skill.getId(), skill.getPreferredLabel(), link.getRequirement());
            switch (link.getRequirement()) {
                case REQUIRED -> required.add(view);
                case PREFERRED -> preferred.add(view);
            }
        }
        Comparator<JobSkillView> byLabel = Comparator.comparing(JobSkillView::preferredLabel, String.CASE_INSENSITIVE_ORDER);
        required.sort(byLabel);
        preferred.sort(byLabel);
        return JobDetailView.from(job, required, preferred);
    }

    @CacheEvict(cacheNames = CacheConfig.JOB_FEED, allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new JobNotFoundException(id);
        }
        jobSkillRepository.deleteByIdJobId(id);
        jobRepository.deleteById(id);
    }

    @CacheEvict(cacheNames = CacheConfig.JOB_FEED, allEntries = true)
    @Transactional
    public int deleteAll(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<UUID> distinct = ids.stream().distinct().toList();
        List<Job> jobs = jobRepository.findAllById(distinct);
        if (jobs.isEmpty()) {
            return 0;
        }
        List<UUID> jobIds = jobs.stream().map(Job::getId).toList();
        jobSkillRepository.deleteByIdJobIdIn(jobIds);
        jobRepository.deleteAll(jobs);
        return jobs.size();
    }

    private static String normaliseCountry(String country) {
        if (country == null || country.isBlank()) return "";
        return country.trim().toUpperCase(Locale.ROOT);
    }

    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(UUID id) {
            super("Job not found: " + id);
        }
    }
}
