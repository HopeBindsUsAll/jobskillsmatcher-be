package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.matching.ReadinessService;

import com.jobskillsmatcher.matching.JobFeedService;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.impl.jpa.JobSkillId;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.job.model.Seniority;
import com.jobskillsmatcher.matching.MatchingProperties;
import com.jobskillsmatcher.matching.ReadinessSnapshotRepository;
import com.jobskillsmatcher.matching.port.rest.JobFeedItemView;
import com.jobskillsmatcher.skill.SkillTestDataFactory;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkillId;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import com.jobskillsmatcher.studentskill.model.SkillSource;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration-style test that wires the real {@link ReadinessService} +
 * {@link JobFeedService} with mocked JPA repositories and asserts the
 * end-to-end ranking order. One student + five jobs with hand-controlled
 * skill overlap so the expected order can be reasoned about by hand.
 */
class MatchingServiceIntegrationTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UUID jobAllMatch = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final UUID jobMostMatch = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private final UUID jobHalfMatch = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    private final UUID jobOneMatch = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    private final UUID jobNoMatch = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");

    private StudentSkillRepository studentSkillRepository;
    private JobRepository jobRepository;
    private JobSkillRepository jobSkillRepository;
    private SkillRepository skillRepository;
    private StudentProfileRepository studentProfileRepository;
    private ReadinessSnapshotRepository readinessSnapshotRepository;
    private JobFeedServiceImpl jobFeedService;

    @BeforeEach
    void setUp() {
        studentSkillRepository = mock(StudentSkillRepository.class);
        jobRepository = mock(JobRepository.class);
        jobSkillRepository = mock(JobSkillRepository.class);
        skillRepository = mock(SkillRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        readinessSnapshotRepository = mock(ReadinessSnapshotRepository.class);

        MatchingProperties props = new MatchingProperties();
        ReadinessService readinessService = new ReadinessServiceImpl(
                studentSkillRepository, jobRepository, jobSkillRepository, skillRepository, props);
        jobFeedService = new JobFeedServiceImpl(
                jobRepository, readinessService, studentProfileRepository, readinessSnapshotRepository);

        seedSkillCatalog();
        seedStudent();
        seedFiveJobs();
    }

    @Test
    void successFeedRanksBySkillOverlapDescending() {
        Page<JobFeedItemView> page = jobFeedService.feed(STUDENT_ID, "", "", false, null, "", 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(5);
        // Score must decrease monotonically.
        for (int i = 1; i < page.getContent().size(); i++) {
            assertThat(page.getContent().get(i).score())
                    .isLessThanOrEqualTo(page.getContent().get(i - 1).score());
        }
        assertThat(page.getContent().get(0).id()).isEqualTo(jobAllMatch);
        assertThat(page.getContent().get(1).id()).isEqualTo(jobMostMatch);
        // jobNoMatch is last and has score 0.
        JobFeedItemView last = page.getContent().get(page.getContent().size() - 1);
        assertThat(last.id()).isEqualTo(jobNoMatch);
        assertThat(last.score()).isEqualTo(0.0);
    }

    @Test
    void successFeedRespectsSeniorityFilter() {
        // Re-stub the pool with a seniority filter so only matching jobs are returned.
        when(jobRepository.rankingPool(any(), any(), anyBoolean(), eq(Seniority.SENIOR), any(), any(Pageable.class)))
                .thenReturn(List.of(buildJob(jobAllMatch, "Senior Java Engineer", Seniority.SENIOR)));

        Page<JobFeedItemView> page = jobFeedService.feed(STUDENT_ID, "", "", false, Seniority.SENIOR, "", 0, 10);

        assertThat(page.getContent()).extracting(JobFeedItemView::id).containsExactly(jobAllMatch);
    }

    @Test
    void successHeadlineAveragesTopJobs() {
        when(studentProfileRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(profile("MY", "Backend Engineer")));

        var headline = jobFeedService.headline(STUDENT_ID);

        assertThat(headline.sampleSize()).isEqualTo(5);
        assertThat(headline.country()).isEqualTo("MY");
        assertThat(headline.preferredRole()).isEqualTo("Backend Engineer");
        assertThat(headline.topJobs()).isNotEmpty();
        // Top job is the all-match job; its score must dominate.
        assertThat(headline.topJobs().get(0).id()).isEqualTo(jobAllMatch);
        // Average is in [0, 1].
        assertThat(headline.score()).isBetween(0.0, 1.0);
    }

    @Test
    void successFeedEmptyPoolReturnsEmptyPage() {
        when(jobRepository.rankingPool(any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        Page<JobFeedItemView> page = jobFeedService.feed(STUDENT_ID, "ZZ", "", false, null, "", 0, 10);

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    private void seedSkillCatalog() {
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0, 0.0});
        Skill python = withVector(SkillTestDataFactory.python(), new double[]{0.0, 0.0, 1.0});

        when(skillRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(java, sql, python));
    }

    private void seedStudent() {
        // Student is EXPERT in both Java and SQL so a job with both as REQUIRED
        // beats a job that only requires Java (lower-weight overlap on the second
        // skill shrinks Jaccard).
        when(studentSkillRepository.findAllByIdStudentId(STUDENT_ID)).thenReturn(List.of(
                studentSkill(SkillTestDataFactory.JAVA_ID, ProficiencyLevel.EXPERT),
                studentSkill(SkillTestDataFactory.SQL_ID, ProficiencyLevel.EXPERT)
        ));
    }

    private void seedFiveJobs() {
        // Job A: requires Java + SQL → student matches all required skills.
        // Job B: requires Java, prefers SQL → all matched but SQL is preferred not required.
        // Job C: requires Java + Python → half match.
        // Job D: requires Python only, prefers SQL → student matches only the preferred SQL.
        // Job E: requires Python only → no match at all.
        Map<UUID, List<JobSkill>> jobLinks = new HashMap<>();
        jobLinks.put(jobAllMatch, List.of(
                jobSkill(jobAllMatch, SkillTestDataFactory.JAVA_ID, Requirement.REQUIRED),
                jobSkill(jobAllMatch, SkillTestDataFactory.SQL_ID, Requirement.REQUIRED)
        ));
        jobLinks.put(jobMostMatch, List.of(
                jobSkill(jobMostMatch, SkillTestDataFactory.JAVA_ID, Requirement.REQUIRED),
                jobSkill(jobMostMatch, SkillTestDataFactory.SQL_ID, Requirement.PREFERRED)
        ));
        jobLinks.put(jobHalfMatch, List.of(
                jobSkill(jobHalfMatch, SkillTestDataFactory.JAVA_ID, Requirement.REQUIRED),
                jobSkill(jobHalfMatch, SkillTestDataFactory.PYTHON_ID, Requirement.REQUIRED)
        ));
        jobLinks.put(jobOneMatch, List.of(
                jobSkill(jobOneMatch, SkillTestDataFactory.PYTHON_ID, Requirement.REQUIRED),
                jobSkill(jobOneMatch, SkillTestDataFactory.SQL_ID, Requirement.PREFERRED)
        ));
        jobLinks.put(jobNoMatch, List.of(
                jobSkill(jobNoMatch, SkillTestDataFactory.PYTHON_ID, Requirement.REQUIRED)
        ));

        List<Job> pool = List.of(
                buildJob(jobAllMatch, "All match", Seniority.MID),
                buildJob(jobMostMatch, "Most match", Seniority.MID),
                buildJob(jobHalfMatch, "Half match", Seniority.MID),
                buildJob(jobOneMatch, "One match", Seniority.MID),
                buildJob(jobNoMatch, "No match", Seniority.MID)
        );
        when(jobRepository.rankingPool(any(), any(), anyBoolean(), eq(null), any(), any(Pageable.class)))
                .thenReturn(pool);

        List<JobSkill> allLinks = new ArrayList<>();
        for (List<JobSkill> v : jobLinks.values()) allLinks.addAll(v);
        when(jobSkillRepository.findAllByIdJobIdIn(any())).thenReturn(allLinks);
    }

    private Job buildJob(UUID id, String title, Seniority seniority) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setCompany("ACME");
        job.setCountry("MY");
        job.setCity("Kuala Lumpur");
        job.setSeniority(seniority);
        job.setPostedAt(OffsetDateTime.now());
        return job;
    }

    private static Skill withVector(Skill s, double[] v) {
        s.setTfidfVector(v);
        return s;
    }

    private static StudentSkill studentSkill(String skillId, ProficiencyLevel level) {
        StudentSkill e = new StudentSkill();
        e.setId(new StudentSkillId(STUDENT_ID, skillId));
        e.setProficiency(level);
        e.setSource(SkillSource.MANUAL);
        return e;
    }

    private static JobSkill jobSkill(UUID jobId, String skillId, Requirement requirement) {
        JobSkill e = new JobSkill();
        e.setId(new JobSkillId(jobId, skillId));
        e.setRequirement(requirement);
        return e;
    }

    private static StudentProfile profile(String country, String preferredRole) {
        StudentProfile p = new StudentProfile();
        p.setUserId(STUDENT_ID);
        p.setCountry(country);
        p.setPreferredRole(preferredRole);
        return p;
    }
}
