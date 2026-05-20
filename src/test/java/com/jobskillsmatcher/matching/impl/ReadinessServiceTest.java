package com.jobskillsmatcher.matching.impl;

import com.jobskillsmatcher.matching.ReadinessService;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.impl.jpa.JobSkillId;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.matching.MatchingProperties;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;
import com.jobskillsmatcher.skill.SkillTestDataFactory;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkillId;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import com.jobskillsmatcher.studentskill.model.SkillSource;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadinessServiceTest {

    private static final Offset<Double> EPS = Offset.offset(1e-6);
    private static final UUID STUDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID JOB_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock StudentSkillRepository studentSkillRepository;
    @Mock JobRepository jobRepository;
    @Mock JobSkillRepository jobSkillRepository;
    @Mock SkillRepository skillRepository;

    MatchingProperties props;
    ReadinessService service;

    @BeforeEach
    void setUp() {
        props = new MatchingProperties();
        service = new ReadinessServiceImpl(
                studentSkillRepository, jobRepository, jobSkillRepository, skillRepository, props);
    }

    @Test
    void successPerfectMatchProducesNearOne() {
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.REQUIRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.EXPERT),
                studentSkill(STUDENT_ID, sql.getId(), ProficiencyLevel.EXPERT)
        ));
        stubCatalog(java, sql);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        // Both sides have weight 1.0 on every skill → Jaccard 1.0, cosine 1.0, final 1.0.
        assertThat(b.jaccard()).isCloseTo(1.0, EPS);
        assertThat(b.cosine()).isCloseTo(1.0, EPS);
        assertThat(b.score()).isCloseTo(1.0, EPS);
        assertThat(b.matchedSkills()).extracting("skillId")
                .containsExactlyInAnyOrder(java.getId(), sql.getId());
        assertThat(b.missingRequired()).isEmpty();
        assertThat(b.missingPreferred()).isEmpty();
    }

    @Test
    void successCompositionFormulaAlphaJPlusOneMinusAlphaC() {
        // Set α to a non-default value to confirm the composition.
        props.setAlpha(0.75);
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.PREFERRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.INTERMEDIATE)
        ));
        stubCatalog(java, sql);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.score()).isCloseTo(0.75 * b.jaccard() + 0.25 * b.cosine(), EPS);
    }

    @Test
    void successCategorisesMatchedAndMissingSkills() {
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0});
        Skill python = withVector(SkillTestDataFactory.python(), new double[]{0.5, 0.5});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, python.getId(), Requirement.PREFERRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.ADVANCED)
        ));
        stubCatalog(java, sql, python);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.matchedSkills()).extracting("skillId").containsExactly(java.getId());
        assertThat(b.missingRequired()).extracting("skillId").containsExactly(sql.getId());
        assertThat(b.missingPreferred()).extracting("skillId").containsExactly(python.getId());
    }

    @Test
    void preferredBucketStaysEmpty_whenJobHasNoPreferredSkills() {
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.REQUIRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.ADVANCED)
        ));
        stubCatalog(java, sql);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.missingPreferred())
                .as("a job with zero preferred skills must surface zero preferred gaps")
                .isEmpty();
        assertThat(b.missingRequired()).extracting("skillId").containsExactly(sql.getId());
    }

    @Test
    void preferredBucketCorrectlyPartitionedFromRequired() {
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0});
        Skill python = withVector(SkillTestDataFactory.python(), new double[]{0.5, 0.5});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.PREFERRED),
                jobSkill(JOB_ID, python.getId(), Requirement.PREFERRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of()); // student has nothing
        stubCatalog(java, sql, python);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.missingRequired()).extracting("skillId").containsExactly(java.getId());
        assertThat(b.missingPreferred()).extracting("skillId")
                .containsExactlyInAnyOrder(sql.getId(), python.getId());
    }

    @Test
    void successUnknownJobReturnsEmptyBreakdown() {
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.score()).isCloseTo(0.0, EPS);
        assertThat(b.jaccard()).isCloseTo(0.0, EPS);
        assertThat(b.cosine()).isCloseTo(0.0, EPS);
        assertThat(b.matchedSkills()).isEmpty();
        assertThat(b.missingRequired()).isEmpty();
        assertThat(b.missingPreferred()).isEmpty();
    }

    @Test
    void coverageModeIgnoresExtraUnrelatedStudentSkills() {
        // Job wants java + sql. Student has java + sql (perfect match) AND an unrelated python.
        // With coverage mode (default), the python skill must not lower the score.
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0, 0.0});
        Skill python = withVector(SkillTestDataFactory.python(), new double[]{0.0, 0.0, 1.0});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.REQUIRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.EXPERT),
                studentSkill(STUDENT_ID, sql.getId(), ProficiencyLevel.EXPERT),
                studentSkill(STUDENT_ID, python.getId(), ProficiencyLevel.EXPERT)
        ));
        stubCatalog(java, sql, python);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.jaccard()).isCloseTo(1.0, EPS);
        assertThat(b.cosine()).isCloseTo(1.0, EPS);
        assertThat(b.score()).isCloseTo(1.0, EPS);
    }

    @Test
    void legacyModeStillPenalisesExtraSkillsForBackcompat() {
        // Same setup, but coverage mode disabled — old symmetric behaviour returns.
        // The unrelated python both inflates the Jaccard union and drags the cosine vector
        // toward an irrelevant axis, so neither component should reach 1.0.
        props.setCoverageMode(false);
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0, 0.0});
        Skill python = withVector(SkillTestDataFactory.python(), new double[]{0.0, 0.0, 1.0});
        stubJob(JOB_ID);
        stubJobSkills(JOB_ID, List.of(
                jobSkill(JOB_ID, java.getId(), Requirement.REQUIRED),
                jobSkill(JOB_ID, sql.getId(), Requirement.REQUIRED)
        ));
        stubStudentSkills(STUDENT_ID, List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.EXPERT),
                studentSkill(STUDENT_ID, sql.getId(), ProficiencyLevel.EXPERT),
                studentSkill(STUDENT_ID, python.getId(), ProficiencyLevel.EXPERT)
        ));
        stubCatalog(java, sql, python);

        ScoreBreakdown b = service.score(STUDENT_ID, JOB_ID);

        assertThat(b.jaccard()).isLessThan(1.0);
        assertThat(b.cosine()).isLessThan(1.0);
    }

    @Test
    void successScoreManyComputesAllJobsInOnePass() {
        UUID jobA = UUID.randomUUID();
        UUID jobB = UUID.randomUUID();
        Skill java = withVector(SkillTestDataFactory.java(), new double[]{1.0, 0.0});
        Skill sql = withVector(SkillTestDataFactory.sql(), new double[]{0.0, 1.0});

        when(studentSkillRepository.findAllByIdStudentId(STUDENT_ID)).thenReturn(List.of(
                studentSkill(STUDENT_ID, java.getId(), ProficiencyLevel.EXPERT)
        ));
        when(jobSkillRepository.findAllByIdJobIdIn(List.of(jobA, jobB))).thenReturn(List.of(
                jobSkill(jobA, java.getId(), Requirement.REQUIRED),
                jobSkill(jobB, sql.getId(), Requirement.REQUIRED)
        ));
        when(skillRepository.findAllById(any(Iterable.class))).thenReturn(List.of(java, sql));

        var out = service.scoreMany(STUDENT_ID, List.of(jobA, jobB));

        assertThat(out).containsKeys(jobA, jobB);
        assertThat(out.get(jobA).score()).isGreaterThan(out.get(jobB).score());
    }

    private void stubJob(UUID jobId) {
        Job job = new Job();
        job.setId(jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
    }

    private void stubJobSkills(UUID jobId, List<JobSkill> skills) {
        when(jobSkillRepository.findAllByIdJobId(jobId)).thenReturn(skills);
    }

    private void stubStudentSkills(UUID studentId, List<StudentSkill> skills) {
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(skills);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubCatalog(Skill... skills) {
        lenient().when(skillRepository.findAllById((Iterable) any())).thenReturn(List.of(skills));
    }

    private static Skill withVector(Skill s, double[] v) {
        s.setTfidfVector(v);
        return s;
    }

    private static StudentSkill studentSkill(UUID studentId, String skillId, ProficiencyLevel level) {
        StudentSkill e = new StudentSkill();
        e.setId(new StudentSkillId(studentId, skillId));
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
}
