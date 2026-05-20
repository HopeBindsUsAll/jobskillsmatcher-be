package com.jobskillsmatcher.recommendation.impl;

import com.jobskillsmatcher.recommendation.RecommendationService;

import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.matching.ReadinessService;
import com.jobskillsmatcher.matching.model.MatchedSkill;
import com.jobskillsmatcher.matching.model.MissingSkill;
import com.jobskillsmatcher.matching.model.ScoreBreakdown;
import com.jobskillsmatcher.recommendation.model.RecommendationReason;
import com.jobskillsmatcher.recommendation.model.SkillRecommendation;
import com.jobskillsmatcher.recommendation.port.rest.JobRecommendationsView;
import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.LearningResourceRepository;
import com.jobskillsmatcher.resource.impl.jpa.ResourceSkill;
import com.jobskillsmatcher.resource.ResourceSkillRepository;
import com.jobskillsmatcher.resource.model.ResourceDifficulty;
import com.jobskillsmatcher.resource.model.ResourceType;
import com.jobskillsmatcher.resource.port.rest.ResourceSkillRef;
import com.jobskillsmatcher.resource.port.rest.ResourceView;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Picks learning resources to close the student's skill gaps for a given job.
// Ranks by type (COURSE > VIDEO > BOOK > ARTICLE), then by closeness to the target level.
// Dead URLs are excluded by the repository query.
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    static final int TOP_N_PER_SKILL = 3;
    private static final ProficiencyLevel REQUIRED_THRESHOLD = ProficiencyLevel.INTERMEDIATE;
    private static final ProficiencyLevel PREFERRED_THRESHOLD = ProficiencyLevel.BEGINNER;

    private final ReadinessService readinessService;
    private final StudentSkillRepository studentSkillRepository;
    private final LearningResourceRepository resourceRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public JobRecommendationsView forJob(UUID studentId, UUID jobId) {
        ScoreBreakdown breakdown = readinessService.score(studentId, jobId);
        ResourceDifficulty studentLevel = inferStudentLevel(studentId);

        List<Gap> requiredGaps = new ArrayList<>();
        for (MissingSkill m : breakdown.missingRequired()) {
            requiredGaps.add(Gap.missing(m, REQUIRED_THRESHOLD));
        }
        for (MatchedSkill m : breakdown.matchedSkills()) {
            if (m.requirement() == Requirement.REQUIRED
                    && ordinal(m.proficiency()) < ordinal(REQUIRED_THRESHOLD)) {
                requiredGaps.add(Gap.underLeveled(m, REQUIRED_THRESHOLD));
            }
        }
        // Preferred threshold is BEGINNER — owning the skill at any level counts.
        List<Gap> preferredGaps = new ArrayList<>();
        for (MissingSkill m : breakdown.missingPreferred()) {
            preferredGaps.add(Gap.missing(m, PREFERRED_THRESHOLD));
        }

        List<SkillRecommendation> required = recommendFor(requiredGaps, studentLevel);
        List<SkillRecommendation> preferred = recommendFor(preferredGaps, studentLevel);
        return new JobRecommendationsView(jobId, required, preferred);
    }

    private List<SkillRecommendation> recommendFor(List<Gap> gaps, ResourceDifficulty studentLevel) {
        if (gaps.isEmpty()) return List.of();

        Set<String> skillIds = new HashSet<>();
        for (Gap g : gaps) skillIds.add(g.skillId());

        List<LearningResource> candidates = resourceRepository.findAllForSkillIds(skillIds);
        Map<UUID, LearningResource> candidateById = new HashMap<>();
        for (LearningResource r : candidates) candidateById.put(r.getId(), r);

        List<ResourceSkill> links = resourceSkillRepository.findAllByIdSkillIdIn(skillIds);
        Map<String, List<UUID>> resourcesBySkill = new HashMap<>();
        Set<UUID> referencedResourceIds = new HashSet<>();
        Set<String> referencedSkillIds = new HashSet<>();
        for (ResourceSkill link : links) {
            UUID rid = link.getId().getResourceId();
            String sid = link.getId().getSkillId();
            if (!candidateById.containsKey(rid)) continue;
            resourcesBySkill.computeIfAbsent(sid, k -> new ArrayList<>()).add(rid);
            referencedResourceIds.add(rid);
            referencedSkillIds.add(sid);
        }

        List<ResourceSkill> allLinksForCandidates = referencedResourceIds.isEmpty()
                ? List.of()
                : resourceSkillRepository.findAllByIdResourceIdIn(referencedResourceIds);
        for (ResourceSkill l : allLinksForCandidates) referencedSkillIds.add(l.getId().getSkillId());

        Map<String, String> skillLabels = new HashMap<>();
        if (!referencedSkillIds.isEmpty()) {
            for (Skill s : skillRepository.findAllById(referencedSkillIds)) {
                skillLabels.put(s.getId(), s.getPreferredLabel());
            }
        }

        Map<UUID, List<ResourceSkillRef>> skillsByResource = new HashMap<>();
        for (ResourceSkill l : allLinksForCandidates) {
            String sid = l.getId().getSkillId();
            skillsByResource.computeIfAbsent(l.getId().getResourceId(), k -> new ArrayList<>())
                    .add(new ResourceSkillRef(sid, skillLabels.getOrDefault(sid, sid)));
        }
        for (List<ResourceSkillRef> list : skillsByResource.values()) {
            list.sort(Comparator.comparing(ResourceSkillRef::preferredLabel, String.CASE_INSENSITIVE_ORDER));
        }

        // MISSING before UNDER_LEVELED, then alpha for stable output.
        gaps.sort(Comparator
                .comparing((Gap g) -> g.reason() == RecommendationReason.MISSING ? 0 : 1)
                .thenComparing(Gap::preferredLabel, String.CASE_INSENSITIVE_ORDER));

        List<SkillRecommendation> out = new ArrayList<>(gaps.size());
        for (Gap gap : gaps) {
            List<UUID> resourceIds = resourcesBySkill.getOrDefault(gap.skillId(), List.of());
            List<ResourceView> ranked = new ArrayList<>();
            List<LearningResource> entities = new ArrayList<>(resourceIds.size());
            for (UUID rid : resourceIds) {
                LearningResource r = candidateById.get(rid);
                if (r != null) entities.add(r);
            }
            // Under-leveled gaps rank against the target, not the student's overall level.
            ResourceDifficulty rankLevel = gap.reason() == RecommendationReason.UNDER_LEVELED
                    ? toResourceDifficulty(gap.targetLevel())
                    : studentLevel;
            entities.sort(RESOURCE_RANK(rankLevel));
            int limit = Math.min(TOP_N_PER_SKILL, entities.size());
            for (int i = 0; i < limit; i++) {
                LearningResource e = entities.get(i);
                ranked.add(ResourceView.from(e, skillsByResource.getOrDefault(e.getId(), List.of())));
            }
            out.add(new SkillRecommendation(
                    gap.skillId(),
                    gap.preferredLabel(),
                    gap.requirement(),
                    gap.reason(),
                    gap.currentLevel(),
                    gap.targetLevel(),
                    ranked));
        }
        return out;
    }

    // Type priority first, then difficulty closest to level+1, then title.
    // Resources harder than level+1 are pushed to the back.
    static Comparator<LearningResource> RESOURCE_RANK(ResourceDifficulty level) {
        int cap = ordinal(level) + 1;
        return Comparator
                .comparingInt((LearningResource r) -> typeRank(r.getType()))
                .thenComparingInt(r -> ordinal(r.getDifficulty()) > cap ? 1 : 0)
                .thenComparingInt(r -> Math.abs(ordinal(r.getDifficulty()) - cap))
                .thenComparing(LearningResource::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private static int typeRank(ResourceType type) {
        return switch (type) {
            case COURSE -> 0;
            case VIDEO -> 1;
            case BOOK -> 2;
            case ARTICLE -> 3;
        };
    }

    private static int ordinal(ResourceDifficulty d) {
        return switch (d) {
            case BEGINNER -> 0;
            case INTERMEDIATE -> 1;
            case ADVANCED -> 2;
        };
    }

    private static int ordinal(ProficiencyLevel p) {
        return switch (p) {
            case BEGINNER -> 0;
            case INTERMEDIATE -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }

    private static ResourceDifficulty toResourceDifficulty(ProficiencyLevel p) {
        return switch (p) {
            case BEGINNER -> ResourceDifficulty.BEGINNER;
            case INTERMEDIATE -> ResourceDifficulty.INTERMEDIATE;
            case ADVANCED, EXPERT -> ResourceDifficulty.ADVANCED;
        };
    }

    // EXPERT collapses to ADVANCED — no expert-only resources in the catalog.
    ResourceDifficulty inferStudentLevel(UUID studentId) {
        List<StudentSkill> rows = studentSkillRepository.findAllByIdStudentId(studentId);
        if (rows.isEmpty()) return ResourceDifficulty.BEGINNER;
        double sum = 0;
        for (StudentSkill s : rows) {
            sum += ordinal(s.getProficiency());
        }
        double avg = sum / rows.size();
        if (avg < 0.7) return ResourceDifficulty.BEGINNER;
        if (avg < 1.7) return ResourceDifficulty.INTERMEDIATE;
        return ResourceDifficulty.ADVANCED;
    }

    private record Gap(
            String skillId,
            String preferredLabel,
            Requirement requirement,
            RecommendationReason reason,
            ProficiencyLevel currentLevel,
            ProficiencyLevel targetLevel
    ) {
        static Gap missing(MissingSkill m, ProficiencyLevel target) {
            return new Gap(m.skillId(), m.preferredLabel(), m.requirement(),
                    RecommendationReason.MISSING, null, target);
        }

        static Gap underLeveled(MatchedSkill m, ProficiencyLevel target) {
            return new Gap(m.skillId(), m.preferredLabel(), m.requirement(),
                    RecommendationReason.UNDER_LEVELED, m.proficiency(), target);
        }
    }
}
