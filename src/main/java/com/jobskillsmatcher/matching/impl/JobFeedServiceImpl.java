package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.matching.JobFeedService;
import com.jobskillsmatcher.matching.ReadinessService;

import com.jobskillsmatcher.context.cache.CacheConfig;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.JobServiceImpl.JobNotFoundException;
import com.jobskillsmatcher.job.model.Seniority;
import com.jobskillsmatcher.matching.impl.jpa.ReadinessSnapshot;
import com.jobskillsmatcher.matching.ReadinessSnapshotRepository;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;
import com.jobskillsmatcher.matching.port.rest.JobFeedItemView;
import com.jobskillsmatcher.matching.port.rest.JobScoredDetailView;
import com.jobskillsmatcher.matching.port.rest.ReadinessHeadlineView;
import com.jobskillsmatcher.matching.port.rest.ReadinessTrendView;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobFeedServiceImpl implements JobFeedService {

    // Cap the ranking pool — scoring is O(pool * skills).
    static final int RANKING_POOL_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int HEADLINE_TOP_N = 10;
    private static final int TREND_MAX_POINTS = 30;

    private final JobRepository jobRepository;
    private final ReadinessService readinessService;
    private final StudentProfileRepository studentProfileRepository;
    private final ReadinessSnapshotRepository readinessSnapshotRepository;

    @Cacheable(value = CacheConfig.JOB_FEED,
            key = "'feed:' + #studentId + ':' + (#country == null ? '' : #country) + ':' + (#city == null ? '' : #city)"
                    + " + ':' + #remoteOnly + ':' + (#seniority == null ? '' : #seniority.name())"
                    + " + ':' + (#search == null ? '' : #search) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<JobFeedItemView> feed(UUID studentId,
                                      String country,
                                      String city,
                                      boolean remoteOnly,
                                      Seniority seniority,
                                      String search,
                                      int page,
                                      int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        List<Job> pool = jobRepository.rankingPool(
                normaliseCountry(country),
                city == null ? "" : city,
                remoteOnly,
                seniority,
                search == null ? "" : search.trim(),
                PageRequest.of(0, RANKING_POOL_SIZE));
        if (pool.isEmpty()) {
            return Page.empty(PageRequest.of(safePage, safeSize));
        }
        List<JobFeedItemView> ranked = scoreAndRank(studentId, pool);
        // Slice manually so every page paginates over the same scored pool.
        int from = Math.min(safePage * safeSize, ranked.size());
        int to = Math.min(from + safeSize, ranked.size());
        List<JobFeedItemView> slice = ranked.subList(from, to);
        return new PageImpl<>(slice, PageRequest.of(safePage, safeSize), ranked.size());
    }

    @Transactional(readOnly = true)
    public JobScoredDetailView detail(UUID studentId, UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        ScoreBreakdown breakdown = readinessService.score(studentId, jobId);
        return JobScoredDetailView.from(job, breakdown);
    }

    // Headline = average score over the top N jobs in the student's country.
    // Falls back to all-country when the profile is empty.
    @Cacheable(value = CacheConfig.JOB_FEED, key = "'headline:' + #studentId")
    @Transactional(readOnly = true)
    public ReadinessHeadlineView headline(UUID studentId) {
        StudentProfile profile = studentProfileRepository.findById(studentId).orElse(null);
        String country = profile == null ? null : profile.getCountry();
        String preferredRole = profile == null ? null : profile.getPreferredRole();

        List<Job> pool = jobRepository.rankingPool(
                normaliseCountry(country), "", false, null, "",
                PageRequest.of(0, RANKING_POOL_SIZE));
        if (pool.isEmpty()) {
            pool = jobRepository.rankingPool("", "", false, null, "",
                    PageRequest.of(0, RANKING_POOL_SIZE));
        }
        if (pool.isEmpty()) {
            return new ReadinessHeadlineView(0.0, 0, country, preferredRole, List.of());
        }
        List<JobFeedItemView> ranked = scoreAndRank(studentId, pool);
        int n = Math.min(HEADLINE_TOP_N, ranked.size());
        double sum = 0.0;
        List<ReadinessHeadlineView.TopJob> top = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            JobFeedItemView v = ranked.get(i);
            sum += v.score();
            top.add(new ReadinessHeadlineView.TopJob(v.id(), v.title(), v.company(), v.score()));
        }
        double avg = n == 0 ? 0.0 : sum / n;
        return new ReadinessHeadlineView(avg, n, country, preferredRole, top);
    }

    // Returns snapshots oldest-first so the chart plots left-to-right.
    @Transactional(readOnly = true)
    public ReadinessTrendView trend(UUID studentId) {
        List<ReadinessSnapshot> rows = readinessSnapshotRepository
                .findAllByStudentIdOrderByCapturedAtDesc(studentId,
                        PageRequest.of(0, TREND_MAX_POINTS));
        List<ReadinessTrendView.Point> points = new ArrayList<>(rows.size());
        for (int i = rows.size() - 1; i >= 0; i--) {
            ReadinessSnapshot row = rows.get(i);
            UUID[] top = row.getTopJobIds() == null ? new UUID[0] : row.getTopJobIds();
            points.add(new ReadinessTrendView.Point(
                    row.getId(), row.getCapturedAt(), row.getScore(), List.of(top)));
        }
        return new ReadinessTrendView(points);
    }

    @Transactional(readOnly = true)
    public List<UUID> snapshotTopJobIds(UUID studentId, String country, int limit) {
        List<Job> pool = jobRepository.rankingPool(
                normaliseCountry(country), "", false, null, "",
                PageRequest.of(0, RANKING_POOL_SIZE));
        if (pool.isEmpty()) {
            return List.of();
        }
        List<JobFeedItemView> ranked = scoreAndRank(studentId, pool);
        int n = Math.min(limit, ranked.size());
        List<UUID> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(ranked.get(i).id());
        }
        return out;
    }

    private List<JobFeedItemView> scoreAndRank(UUID studentId, List<Job> pool) {
        List<UUID> ids = pool.stream().map(Job::getId).toList();
        Map<UUID, ScoreBreakdown> scores = readinessService.scoreMany(studentId, ids);
        List<JobFeedItemView> out = new ArrayList<>(pool.size());
        for (Job job : pool) {
            ScoreBreakdown b = scores.getOrDefault(job.getId(), emptyBreakdown());
            out.add(JobFeedItemView.from(
                    job,
                    b.score(),
                    b.jaccard(),
                    b.cosine(),
                    b.matchedSkills().size(),
                    b.missingRequired().size(),
                    b.missingPreferred().size()));
        }
        out.sort(Comparator.comparingDouble(JobFeedItemView::score).reversed()
                .thenComparing(v -> v.postedAt() == null ? java.time.OffsetDateTime.MIN : v.postedAt(),
                        Comparator.reverseOrder()));
        return out;
    }

    private static ScoreBreakdown emptyBreakdown() {
        return new ScoreBreakdown(0.0, 0.0, 0.0, List.of(), List.of(), List.of());
    }

    private static String normaliseCountry(String country) {
        if (country == null || country.isBlank()) return "";
        return country.trim().toUpperCase(Locale.ROOT);
    }
}
