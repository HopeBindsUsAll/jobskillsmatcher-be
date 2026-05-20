package com.jobskillsmatcher.ingest.impl.groq;

import com.jobskillsmatcher.ingest.impl.JobSkillExtractor;
import com.jobskillsmatcher.ingest.impl.groq.GroqSkillClient.GroqSkill;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class GroqSkillEnricher {

    private final GroqSkillClient groqSkillClient;
    private final JobSkillExtractor jobSkillExtractor;
    private final SkillRepository skillRepository;

    public Map<String, Requirement> enrich(String text, Map<String, Requirement> algoResult) {
        if (algoResult == null) {
            algoResult = Map.of();
        }
        List<String> detectedLabels = labelsFor(algoResult.keySet());

        Optional<List<GroqSkill>> suggestions =
                groqSkillClient.suggestMissingSkills(text, detectedLabels);
        if (suggestions.isEmpty() || suggestions.get().isEmpty()) {
            return algoResult;
        }

        Map<String, Requirement> merged = new LinkedHashMap<>(algoResult);
        int added = 0;
        for (GroqSkill suggestion : suggestions.get()) {
            Optional<String> skillId = jobSkillExtractor.matchLabel(suggestion.skill());
            if (skillId.isEmpty()) {
                continue;
            }
            Requirement req = "REQUIRED".equals(suggestion.requirement())
                    ? Requirement.REQUIRED
                    : Requirement.PREFERRED;
            merged.merge(skillId.get(), req, GroqSkillEnricher::strongest);
            added++;
        }
        if (added > 0) {
            log.debug("Groq added {} skill(s) the algorithm missed", added);
        }
        return merged;
    }

    private List<String> labelsFor(Iterable<String> ids) {
        List<String> labels = new ArrayList<>();
        for (Skill s : skillRepository.findAllById(ids)) {
            if (s.getPreferredLabel() != null && !s.getPreferredLabel().isBlank()) {
                labels.add(s.getPreferredLabel());
            }
        }
        return labels;
    }

    private static Requirement strongest(Requirement a, Requirement b) {
        return (a == Requirement.REQUIRED || b == Requirement.REQUIRED)
                ? Requirement.REQUIRED
                : Requirement.PREFERRED;
    }
}
