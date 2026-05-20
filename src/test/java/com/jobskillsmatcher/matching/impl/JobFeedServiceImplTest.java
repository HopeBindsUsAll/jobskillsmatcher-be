package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.model.Seniority;
import com.jobskillsmatcher.matching.ReadinessService;
import com.jobskillsmatcher.matching.ReadinessSnapshotRepository;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;
import com.jobskillsmatcher.matching.port.rest.JobFeedItemView;
import com.jobskillsmatcher.matching.port.rest.ReadinessHeadlineView;
import com.jobskillsmatcher.user.StudentProfileRepository;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobFeedServiceImplTest {

    @Mock
    JobRepository jobRepository;

    @Mock
    ReadinessService readinessService;

    @Mock
    StudentProfileRepository studentProfileRepository;

    @Mock
    ReadinessSnapshotRepository readinessSnapshotRepository;

    @InjectMocks
    JobFeedServiceImpl jobFeedService;

    @Test
    void successFeedScoresAndRanksJobs() {
        UUID studentId = UUID.randomUUID();
        Job low = job("low", 0.2, OffsetDateTime.now().minusDays(2));
        Job high = job("high", 0.9, OffsetDateTime.now().minusDays(1));
        Job mid = job("mid", 0.5, OffsetDateTime.now());
        when(jobRepository.rankingPool(any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(low, high, mid));
        when(readinessService.scoreMany(eq(studentId), any())).thenReturn(Map.of(
                low.getId(), breakdown(0.2),
                high.getId(), breakdown(0.9),
                mid.getId(), breakdown(0.5)));

        Page<JobFeedItemView> page = jobFeedService.feed(studentId, "US", "", false, null, "", 0, 10);

        assertThat(page.getContent()).extracting(JobFeedItemView::score)
                .containsExactly(0.9, 0.5, 0.2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void successFeedClampsSizeAndPageBounds() {
        UUID studentId = UUID.randomUUID();
        when(jobRepository.rankingPool(any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(job("only", 0.4, OffsetDateTime.now())));
        when(readinessService.scoreMany(eq(studentId), any())).thenReturn(Map.of());

        Page<JobFeedItemView> oversized = jobFeedService.feed(studentId, "", "", false, null, "", -3, 9999);
        Page<JobFeedItemView> undersized = jobFeedService.feed(studentId, "", "", false, null, "", 0, 0);

        // Page size clamped to MAX (50), negative page floored to 0.
        assertThat(oversized.getPageable().getPageSize()).isEqualTo(50);
        assertThat(oversized.getPageable().getPageNumber()).isZero();
        // Page size clamped to MIN (1).
        assertThat(undersized.getPageable().getPageSize()).isEqualTo(1);
    }

    @Test
    void failedFeedReturnsEmptyPageWhenPoolEmpty() {
        UUID studentId = UUID.randomUUID();
        when(jobRepository.rankingPool(any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        Page<JobFeedItemView> page = jobFeedService.feed(studentId, "DE", "", false, null, "", 0, 10);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        verify(readinessService, times(0)).scoreMany(any(), any());
    }

    @Test
    void successHeadlineAveragesTopTenJobsInRegion() {
        UUID studentId = UUID.randomUUID();
        StudentProfile profile = profileFor(studentId, "US", "Backend Engineer");
        when(studentProfileRepository.findById(studentId)).thenReturn(Optional.of(profile));
        List<Job> pool = new ArrayList<>();
        Map<UUID, ScoreBreakdown> scores = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            Job j = job("j" + i, 1.0 - i * 0.05, OffsetDateTime.now().minusDays(i));
            pool.add(j);
            scores.put(j.getId(), breakdown(1.0 - i * 0.05));
        }
        when(jobRepository.rankingPool(eq("US"), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(pool);
        when(readinessService.scoreMany(eq(studentId), any())).thenReturn(scores);

        ReadinessHeadlineView headline = jobFeedService.headline(studentId);

        assertThat(headline.sampleSize()).isEqualTo(10);
        assertThat(headline.topJobs()).hasSize(10);
        // Average of top 10: (1.00 + 0.95 + 0.90 + ... + 0.55) / 10 = 0.775
        assertThat(headline.score()).isCloseTo(0.775, within(1e-6));
        assertThat(headline.country()).isEqualTo("US");
        assertThat(headline.preferredRole()).isEqualTo("Backend Engineer");
    }

    @Test
    void successHeadlineFallsBackToAllCountryWhenFilteredEmpty() {
        UUID studentId = UUID.randomUUID();
        when(studentProfileRepository.findById(studentId))
                .thenReturn(Optional.of(profileFor(studentId, "DE", "Frontend")));
        Job onlyJob = job("only", 0.7, OffsetDateTime.now());
        ArgumentCaptor<String> countryCap = ArgumentCaptor.forClass(String.class);
        when(jobRepository.rankingPool(countryCap.capture(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of()) // filtered pool empty
                .thenReturn(List.of(onlyJob)); // all-country fallback returns 1
        when(readinessService.scoreMany(eq(studentId), any()))
                .thenReturn(Map.of(onlyJob.getId(), breakdown(0.7)));

        ReadinessHeadlineView headline = jobFeedService.headline(studentId);

        assertThat(headline.sampleSize()).isEqualTo(1);
        assertThat(headline.score()).isCloseTo(0.7, within(1e-6));
        // First call: country filter "DE". Second call (fallback): empty country.
        assertThat(countryCap.getAllValues()).containsExactly("DE", "");
    }

    @Test
    void coverageSnapshotTopJobIdsReturnsLimitedList() {
        UUID studentId = UUID.randomUUID();
        Job a = job("a", 0.3, OffsetDateTime.now());
        Job b = job("b", 0.9, OffsetDateTime.now());
        Job c = job("c", 0.6, OffsetDateTime.now());
        when(jobRepository.rankingPool(eq("US"), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(a, b, c));
        when(readinessService.scoreMany(eq(studentId), any())).thenReturn(Map.of(
                a.getId(), breakdown(0.3),
                b.getId(), breakdown(0.9),
                c.getId(), breakdown(0.6)));

        List<UUID> top = jobFeedService.snapshotTopJobIds(studentId, "US", 2);

        assertThat(top).containsExactly(b.getId(), c.getId());
    }

    private static Job job(String externalId, double scoreHint, OffsetDateTime postedAt) {
        Job j = new Job();
        j.setId(UUID.randomUUID());
        j.setExternalId(externalId);
        j.setTitle("Title-" + externalId);
        j.setCompany("Company");
        j.setCountry("US");
        j.setCity("");
        j.setRemote(false);
        j.setSeniority(Seniority.MID);
        j.setDescription("desc");
        j.setSourceUrl("");
        j.setPostedAt(postedAt);
        return j;
    }

    private static ScoreBreakdown breakdown(double score) {
        return new ScoreBreakdown(score, score, score, List.of(), List.of(), List.of());
    }

    private static StudentProfile profileFor(UUID id, String country, String preferredRole) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(id);
        User user = new User();
        user.setId(id);
        profile.setUser(user);
        profile.setCountry(country);
        profile.setPreferredRole(preferredRole);
        return profile;
    }
}
