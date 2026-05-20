package com.jobskillsmatcher.recommendation.impl;

import com.jobskillsmatcher.matching.ReadinessService;

import com.jobskillsmatcher.recommendation.RecommendationService;

import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.matching.ReadinessService;
import com.jobskillsmatcher.matching.model.MissingSkill;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;
import com.jobskillsmatcher.recommendation.model.SkillRecommendation;
import com.jobskillsmatcher.recommendation.port.rest.JobRecommendationsView;
import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.LearningResourceRepository;
import com.jobskillsmatcher.resource.impl.jpa.ResourceSkill;
import com.jobskillsmatcher.resource.impl.jpa.ResourceSkillId;
import com.jobskillsmatcher.resource.ResourceSkillRepository;
import com.jobskillsmatcher.resource.model.ResourceDifficulty;
import com.jobskillsmatcher.resource.model.ResourceType;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkillId;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import com.jobskillsmatcher.studentskill.model.SkillSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private ReadinessService readinessService;
    private StudentSkillRepository studentSkillRepository;
    private LearningResourceRepository resourceRepository;
    private ResourceSkillRepository resourceSkillRepository;
    private SkillRepository skillRepository;
    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        readinessService = mock(ReadinessService.class);
        studentSkillRepository = mock(StudentSkillRepository.class);
        resourceRepository = mock(LearningResourceRepository.class);
        resourceSkillRepository = mock(ResourceSkillRepository.class);
        skillRepository = mock(SkillRepository.class);
        service = new RecommendationServiceImpl(
                readinessService, studentSkillRepository, resourceRepository,
                resourceSkillRepository, skillRepository);
    }

    // ----- inferStudentLevel ---------------------------------------------------

    @Test
    void infer_level_defaults_to_beginner_when_no_skills() {
        UUID studentId = UUID.randomUUID();
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(List.of());
        assertThat(service.inferStudentLevel(studentId))
                .isEqualTo(ResourceDifficulty.BEGINNER);
    }

    static Stream<Arguments> levelByProficiencyMix() {
        return Stream.of(
                // All BEGINNER (avg 0.0) → BEGINNER
                Arguments.of(List.of(ProficiencyLevel.BEGINNER, ProficiencyLevel.BEGINNER),
                        ResourceDifficulty.BEGINNER),
                // Avg 3/4 = 0.75 (just above the 0.7 threshold) → INTERMEDIATE
                Arguments.of(List.of(ProficiencyLevel.INTERMEDIATE, ProficiencyLevel.INTERMEDIATE,
                                ProficiencyLevel.INTERMEDIATE, ProficiencyLevel.BEGINNER),
                        ResourceDifficulty.INTERMEDIATE),
                // Mix of advanced + expert (avg 2.5) → ADVANCED
                Arguments.of(List.of(ProficiencyLevel.ADVANCED, ProficiencyLevel.EXPERT),
                        ResourceDifficulty.ADVANCED),
                // Avg 2/3 ≈ 0.667 (just below 0.7) → still BEGINNER (lower-bucket boundary)
                Arguments.of(List.of(ProficiencyLevel.INTERMEDIATE, ProficiencyLevel.INTERMEDIATE,
                                ProficiencyLevel.BEGINNER),
                        ResourceDifficulty.BEGINNER),
                // Avg 5/3 ≈ 1.667 (just below 1.7) → still INTERMEDIATE (upper-bucket boundary)
                Arguments.of(List.of(ProficiencyLevel.ADVANCED, ProficiencyLevel.ADVANCED,
                                ProficiencyLevel.BEGINNER),
                        ResourceDifficulty.INTERMEDIATE)
        );
    }

    @ParameterizedTest
    @MethodSource("levelByProficiencyMix")
    void infer_level_buckets_average_proficiency(List<ProficiencyLevel> proficiencies,
                                                 ResourceDifficulty expected) {
        UUID studentId = UUID.randomUUID();
        List<StudentSkill> rows = new ArrayList<>();
        int idx = 0;
        for (ProficiencyLevel p : proficiencies) {
            StudentSkill row = new StudentSkill();
            row.setId(new StudentSkillId(studentId, "esco/skill/" + idx++));
            row.setProficiency(p);
            row.setSource(SkillSource.MANUAL);
            rows.add(row);
        }
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(rows);

        assertThat(service.inferStudentLevel(studentId)).isEqualTo(expected);
    }

    // ----- RESOURCE_RANK comparator -------------------------------------------

    @Test
    void rank_prefers_course_then_video_then_book_then_article() {
        LearningResource course = res("course", ResourceType.COURSE, ResourceDifficulty.BEGINNER);
        LearningResource video = res("video", ResourceType.VIDEO, ResourceDifficulty.BEGINNER);
        LearningResource book = res("book", ResourceType.BOOK, ResourceDifficulty.BEGINNER);
        LearningResource article = res("article", ResourceType.ARTICLE, ResourceDifficulty.BEGINNER);

        List<LearningResource> shuffled = new ArrayList<>(List.of(article, book, video, course));
        shuffled.sort(RecommendationServiceImpl.RESOURCE_RANK(ResourceDifficulty.BEGINNER));

        assertThat(shuffled).extracting(LearningResource::getTitle)
                .containsExactly("course", "video", "book", "article");
    }

    @Test
    void rank_pushes_above_cap_resources_to_the_back() {
        // Student is BEGINNER → cap = INTERMEDIATE (ordinal 1).
        LearningResource beginnerVideo = res("Beg video", ResourceType.VIDEO, ResourceDifficulty.BEGINNER);
        LearningResource intermediateVideo = res("Int video", ResourceType.VIDEO, ResourceDifficulty.INTERMEDIATE);
        LearningResource advancedVideo = res("Adv video", ResourceType.VIDEO, ResourceDifficulty.ADVANCED);

        List<LearningResource> shuffled = new ArrayList<>(List.of(advancedVideo, beginnerVideo, intermediateVideo));
        shuffled.sort(RecommendationServiceImpl.RESOURCE_RANK(ResourceDifficulty.BEGINNER));

        // Within cap: closest to cap (INTERMEDIATE, |1-1|=0) before BEGINNER (|0-1|=1).
        // ADVANCED is above cap → goes last.
        assertThat(shuffled).extracting(LearningResource::getTitle)
                .containsExactly("Int video", "Beg video", "Adv video");
    }

    @Test
    void rank_breaks_ties_alphabetically_by_title() {
        LearningResource a = res("Alpha", ResourceType.COURSE, ResourceDifficulty.BEGINNER);
        LearningResource b = res("beta", ResourceType.COURSE, ResourceDifficulty.BEGINNER);
        LearningResource c = res("Gamma", ResourceType.COURSE, ResourceDifficulty.BEGINNER);

        List<LearningResource> shuffled = new ArrayList<>(List.of(c, b, a));
        shuffled.sort(RecommendationServiceImpl.RESOURCE_RANK(ResourceDifficulty.BEGINNER));

        assertThat(shuffled).extracting(LearningResource::getTitle)
                .containsExactly("Alpha", "beta", "Gamma");
    }

    // ----- forJob end-to-end ---------------------------------------------------

    @Test
    void forJob_caps_resources_per_skill_at_top_n() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        MissingSkill gap = new MissingSkill("esco/skill/java", "Java", Requirement.REQUIRED);
        when(readinessService.score(studentId, jobId))
                .thenReturn(new ScoreBreakdown(0, 0, 0, List.of(), List.of(gap), List.of()));
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(List.of());

        List<LearningResource> candidates = new ArrayList<>();
        List<ResourceSkill> links = new ArrayList<>();
        for (int i = 0; i < RecommendationServiceImpl.TOP_N_PER_SKILL + 2; i++) {
            LearningResource r = res("Java course " + i, ResourceType.COURSE, ResourceDifficulty.BEGINNER);
            candidates.add(r);
            links.add(new ResourceSkill(new ResourceSkillId(r.getId(), "esco/skill/java")));
        }
        when(resourceRepository.findAllForSkillIds(any())).thenReturn(candidates);
        when(resourceSkillRepository.findAllByIdSkillIdIn(any())).thenReturn(links);
        when(resourceSkillRepository.findAllByIdResourceIdIn(any())).thenReturn(links);
        when(skillRepository.findAllById(anyCollection()))
                .thenReturn(List.of(skillEntity("esco/skill/java", "Java")));

        JobRecommendationsView view = service.forJob(studentId, jobId);

        assertThat(view.required()).hasSize(1);
        SkillRecommendation javaRec = view.required().get(0);
        assertThat(javaRec.skillId()).isEqualTo("esco/skill/java");
        assertThat(javaRec.resources())
                .as("only top-N resources are surfaced per skill")
                .hasSize(RecommendationServiceImpl.TOP_N_PER_SKILL);
    }

    @Test
    void forJob_orders_required_before_preferred_then_alphabetical_within_bucket() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        MissingSkill required1 = new MissingSkill("esco/skill/sql", "SQL", Requirement.REQUIRED);
        MissingSkill required2 = new MissingSkill("esco/skill/java", "Java", Requirement.REQUIRED);
        MissingSkill preferred1 = new MissingSkill("esco/skill/redis", "Redis", Requirement.PREFERRED);
        MissingSkill preferred2 = new MissingSkill("esco/skill/aws", "AWS", Requirement.PREFERRED);

        when(readinessService.score(studentId, jobId))
                .thenReturn(new ScoreBreakdown(0, 0, 0, List.of(),
                        List.of(required1, required2),
                        List.of(preferred1, preferred2)));
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(List.of());
        when(resourceRepository.findAllForSkillIds(any())).thenReturn(List.of());
        when(resourceSkillRepository.findAllByIdSkillIdIn(any())).thenReturn(List.of());
        when(resourceSkillRepository.findAllByIdResourceIdIn(any())).thenReturn(List.of());
        when(skillRepository.findAllById(anyCollection())).thenReturn(List.of());

        JobRecommendationsView view = service.forJob(studentId, jobId);

        assertThat(view.required()).extracting(SkillRecommendation::preferredLabel)
                .containsExactly("Java", "SQL");
        assertThat(view.preferred()).extracting(SkillRecommendation::preferredLabel)
                .containsExactly("AWS", "Redis");
    }

    @Test
    void forJob_returns_empty_buckets_when_no_gaps() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(readinessService.score(studentId, jobId))
                .thenReturn(new ScoreBreakdown(1, 1, 1, List.of(), List.of(), List.of()));
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(List.of());

        JobRecommendationsView view = service.forJob(studentId, jobId);

        assertThat(view.required()).isEmpty();
        assertThat(view.preferred()).isEmpty();
        assertThat(view.jobId()).isEqualTo(jobId);
    }

    @Test
    void forJob_skips_resources_for_unrelated_skills() {
        UUID studentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        MissingSkill gap = new MissingSkill("esco/skill/java", "Java", Requirement.REQUIRED);
        when(readinessService.score(studentId, jobId))
                .thenReturn(new ScoreBreakdown(0, 0, 0, List.of(), List.of(gap), List.of()));
        when(studentSkillRepository.findAllByIdStudentId(studentId)).thenReturn(List.of());

        // The repository may surface resources tagged with both java and python; only
        // the java link is relevant here. The python-only link must not seep through.
        LearningResource dualTagged = res("Polyglot Course", ResourceType.COURSE, ResourceDifficulty.BEGINNER);
        LearningResource pythonOnly = res("Python Only", ResourceType.COURSE, ResourceDifficulty.BEGINNER);
        when(resourceRepository.findAllForSkillIds(any())).thenReturn(List.of(dualTagged));
        when(resourceSkillRepository.findAllByIdSkillIdIn(any())).thenReturn(List.of(
                new ResourceSkill(new ResourceSkillId(dualTagged.getId(), "esco/skill/java"))
        ));
        when(resourceSkillRepository.findAllByIdResourceIdIn(any())).thenReturn(List.of(
                new ResourceSkill(new ResourceSkillId(dualTagged.getId(), "esco/skill/java")),
                new ResourceSkill(new ResourceSkillId(dualTagged.getId(), "esco/skill/python"))
        ));
        when(skillRepository.findAllById(anyCollection())).thenAnswer(inv -> {
            Collection<String> ids = inv.getArgument(0);
            List<Skill> out = new ArrayList<>();
            for (String id : ids) out.add(skillEntity(id, id.substring(id.lastIndexOf('/') + 1)));
            return out;
        });

        JobRecommendationsView view = service.forJob(studentId, jobId);

        assertThat(view.required()).hasSize(1);
        SkillRecommendation rec = view.required().get(0);
        assertThat(rec.resources()).hasSize(1);
        assertThat(rec.resources().get(0).title()).isEqualTo("Polyglot Course");
        // The full skill list on the embedded resource view should reveal both tags.
        assertThat(rec.resources().get(0).skills())
                .extracting(s -> s.skillId())
                .containsExactlyInAnyOrder("esco/skill/java", "esco/skill/python");
        // pythonOnly must not appear at all — it was never returned by findAllForSkillIds.
        assertThat(rec.resources()).extracting(r -> r.title()).doesNotContain("Python Only");
    }

    // ----- helpers -------------------------------------------------------------

    private static LearningResource res(String title, ResourceType type, ResourceDifficulty difficulty) {
        LearningResource r = new LearningResource();
        r.setId(UUID.randomUUID());
        r.setType(type);
        r.setDifficulty(difficulty);
        r.setTitle(title);
        r.setDescription("");
        r.setUrl("https://example.test/" + title.replace(' ', '-'));
        r.setProvider("Test");
        return r;
    }

    private static Skill skillEntity(String id, String label) {
        Skill s = new Skill();
        s.setId(id);
        s.setPreferredLabel(label);
        s.setAltLabels(new String[]{});
        s.setDescription("");
        return s;
    }
}
