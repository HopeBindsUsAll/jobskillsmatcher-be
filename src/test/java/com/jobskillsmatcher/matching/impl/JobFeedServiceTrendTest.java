package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.matching.ReadinessService;

import com.jobskillsmatcher.matching.JobFeedService;

import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.matching.impl.jpa.ReadinessSnapshot;
import com.jobskillsmatcher.matching.ReadinessSnapshotRepository;
import com.jobskillsmatcher.matching.port.rest.ReadinessTrendView;
import com.jobskillsmatcher.user.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobFeedServiceTrendTest {

    private static final UUID STUDENT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ReadinessService readinessService = mock(ReadinessService.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
    private final ReadinessSnapshotRepository snapshotRepository = mock(ReadinessSnapshotRepository.class);

    private final JobFeedService service = new JobFeedServiceImpl(
            jobRepository, readinessService, studentProfileRepository, snapshotRepository);

    @Test
    void emptyTrendWhenNoSnapshots() {
        when(snapshotRepository.findAllByStudentIdOrderByCapturedAtDesc(eq(STUDENT), any(Pageable.class)))
                .thenReturn(List.of());

        ReadinessTrendView view = service.trend(STUDENT);

        assertThat(view.points()).isEmpty();
    }

    @Test
    void trendReversesToChronologicalOrder() {
        OffsetDateTime t0 = OffsetDateTime.parse("2026-04-01T03:00:00Z");
        ReadinessSnapshot newest = snapshot(0.7, t0.plusDays(2), new UUID[]{newId(), newId()});
        ReadinessSnapshot middle = snapshot(0.6, t0.plusDays(1), new UUID[]{newId()});
        ReadinessSnapshot oldest = snapshot(0.5, t0, new UUID[0]);
        // Repo returns desc; service must flip to oldest-first.
        when(snapshotRepository.findAllByStudentIdOrderByCapturedAtDesc(eq(STUDENT), any(Pageable.class)))
                .thenReturn(List.of(newest, middle, oldest));

        ReadinessTrendView view = service.trend(STUDENT);

        assertThat(view.points()).hasSize(3);
        assertThat(view.points().get(0).score()).isEqualTo(0.5);
        assertThat(view.points().get(1).score()).isEqualTo(0.6);
        assertThat(view.points().get(2).score()).isEqualTo(0.7);
        assertThat(view.points().get(0).topJobIds()).isEmpty();
        assertThat(view.points().get(2).topJobIds()).hasSize(2);
    }

    @Test
    void trendCopesWithNullTopJobsArray() {
        ReadinessSnapshot row = snapshot(0.4, OffsetDateTime.parse("2026-04-10T03:00:00Z"), null);
        when(snapshotRepository.findAllByStudentIdOrderByCapturedAtDesc(eq(STUDENT), any(Pageable.class)))
                .thenReturn(List.of(row));

        ReadinessTrendView view = service.trend(STUDENT);

        assertThat(view.points()).hasSize(1);
        assertThat(view.points().get(0).topJobIds()).isEmpty();
    }

    private static ReadinessSnapshot snapshot(double score, OffsetDateTime at, UUID[] topJobs) {
        ReadinessSnapshot row = new ReadinessSnapshot();
        row.setId(UUID.randomUUID());
        row.setStudentId(STUDENT);
        row.setScore(score);
        row.setCapturedAt(at);
        row.setTopJobIds(topJobs);
        return row;
    }

    private static UUID newId() {
        return UUID.randomUUID();
    }
}
