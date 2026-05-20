package com.jobskillsmatcher.ingest.impl;

import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
// Matches ESCO skill labels against a job description, longest label wins so
// "react native" doesn't also report "react". Section headers (e.g. "nice to have")
// flip hits between REQUIRED and PREFERRED; a skill seen in both wins as REQUIRED.
@Component
@RequiredArgsConstructor
public class JobSkillExtractor {

    private static final List<String> PREFERRED_HEADERS = List.of(
            "nice to have",
            "nice-to-have",
            "would be a plus",
            "would be a bonus",
            "bonus points",
            "bonus skills",
            "bonus if",
            "good to have",
            "preferred qualifications",
            "preferred skills",
            "preferred:",
            "preferred experience",
            "we'd love",
            "we would love",
            "extra credit",
            "pluses"
    );

    private static final List<String> REQUIRED_HEADERS = List.of(
            "requirements",
            "required qualifications",
            "required skills",
            "required:",
            "must have",
            "must-have",
            "responsibilities",
            "what you'll do",
            "what you will do",
            "qualifications",
            "you have",
            "minimum qualifications",
            "key responsibilities"
    );

    private final SkillRepository skillRepository;

    private List<LabelToken> labelIndex = List.of();

    @PostConstruct
    void rebuildIndex() {
        List<Skill> skills = skillRepository.findAll();
        List<LabelToken> tokens = new ArrayList<>();
        for (Skill skill : skills) {
            String preferred = skill.getPreferredLabel();
            if (preferred != null && !preferred.isBlank()) {
                tokens.add(LabelToken.of(skill.getId(), preferred));
            }
            String[] alts = skill.getAltLabels();
            if (alts != null) {
                for (String alt : alts) {
                    if (alt != null && !alt.isBlank()) {
                        tokens.add(LabelToken.of(skill.getId(), alt));
                    }
                }
            }
        }
        // Longest first so "react native" beats "react".
        tokens.sort(Comparator.comparingInt((LabelToken t) -> -t.label.length()));
        labelIndex = List.copyOf(tokens);
    }

    public Map<String, Requirement> extract(String description) {
        if (description == null || description.isBlank() || labelIndex.isEmpty()) {
            return Map.of();
        }
        String haystack = description.toLowerCase(Locale.ROOT);
        List<Section> sections = sectionMap(haystack);

        Map<String, Requirement> bySkill = new HashMap<>();
        boolean[] consumed = new boolean[haystack.length()];

        for (LabelToken token : labelIndex) {
            int from = 0;
            while (from < haystack.length()) {
                int hit = haystack.indexOf(token.lowercaseLabel, from);
                if (hit < 0) {
                    break;
                }
                int end = hit + token.lowercaseLabel.length();
                if (isFreshWholeWord(haystack, hit, end, consumed)) {
                    Requirement req = sectionAt(sections, hit);
                    bySkill.merge(token.skillId, req, JobSkillExtractor::strongest);
                    for (int i = hit; i < end; i++) {
                        consumed[i] = true;
                    }
                }
                from = end;
            }
        }
        return bySkill;
    }

    // Maps a free-text phrase (e.g. an LLM suggestion) back to an ESCO skill id.
    public Optional<String> matchLabel(String phrase) {
        if (phrase == null || phrase.isBlank() || labelIndex.isEmpty()) {
            return Optional.empty();
        }
        String haystack = phrase.toLowerCase(Locale.ROOT);
        for (LabelToken token : labelIndex) {
            int hit = haystack.indexOf(token.lowercaseLabel);
            while (hit >= 0) {
                int end = hit + token.lowercaseLabel.length();
                if (isWholeWord(haystack, hit, end)) {
                    return Optional.of(token.skillId);
                }
                hit = haystack.indexOf(token.lowercaseLabel, hit + 1);
            }
        }
        return Optional.empty();
    }

    private static boolean isWholeWord(String haystack, int start, int end) {
        boolean beforeIsWord = start > 0 && isBoundaryWordChar(haystack, start - 1);
        boolean afterIsWord = end < haystack.length() && isBoundaryWordChar(haystack, end);
        return !beforeIsWord && !afterIsWord;
    }

    private static boolean isFreshWholeWord(String haystack, int start, int end, boolean[] consumed) {
        for (int i = start; i < end; i++) {
            if (consumed[i]) {
                return false;
            }
        }
        return isWholeWord(haystack, start, end);
    }

    private static boolean isBoundaryWordChar(String haystack, int idx) {
        char c = haystack.charAt(idx);
        if (Character.isLetterOrDigit(c) || c == '+' || c == '#') {
            return true;
        }
        if (c == '.') {
            // `.` joins words only when flanked by alphanumerics ("node.js"), not at sentence ends.
            boolean leftAlpha = idx > 0 && Character.isLetterOrDigit(haystack.charAt(idx - 1));
            boolean rightAlpha = idx + 1 < haystack.length() && Character.isLetterOrDigit(haystack.charAt(idx + 1));
            return leftAlpha && rightAlpha;
        }
        return false;
    }

    private static Requirement strongest(Requirement a, Requirement b) {
        return (a == Requirement.REQUIRED || b == Requirement.REQUIRED) ? Requirement.REQUIRED : Requirement.PREFERRED;
    }

    private static List<Section> sectionMap(String lowercaseDescription) {
        // Claim longest headers first so "preferred qualifications" wins over "qualifications".
        List<HeaderCandidate> candidates = new ArrayList<>();
        for (String marker : PREFERRED_HEADERS) {
            candidates.add(new HeaderCandidate(marker, Requirement.PREFERRED));
        }
        for (String marker : REQUIRED_HEADERS) {
            candidates.add(new HeaderCandidate(marker, Requirement.REQUIRED));
        }
        candidates.sort(Comparator.comparingInt((HeaderCandidate c) -> -c.marker.length()));

        boolean[] claimed = new boolean[lowercaseDescription.length()];
        List<Section> sections = new ArrayList<>();
        for (HeaderCandidate c : candidates) {
            int from = 0;
            while (from < lowercaseDescription.length()) {
                int hit = lowercaseDescription.indexOf(c.marker, from);
                if (hit < 0) {
                    break;
                }
                int end = hit + c.marker.length();
                if (!isClaimed(claimed, hit, end)) {
                    sections.add(new Section(hit, c.requirement));
                    for (int i = hit; i < end; i++) {
                        claimed[i] = true;
                    }
                }
                from = end;
            }
        }
        sections.sort(Comparator.comparingInt(s -> s.start));
        return sections;
    }

    private static boolean isClaimed(boolean[] claimed, int start, int end) {
        for (int i = start; i < end; i++) {
            if (claimed[i]) {
                return true;
            }
        }
        return false;
    }

    private record HeaderCandidate(String marker, Requirement requirement) { }

    private static Requirement sectionAt(List<Section> sections, int position) {
        Requirement current = Requirement.REQUIRED;
        for (Section s : sections) {
            if (s.start > position) {
                return current;
            }
            current = s.requirement;
        }
        return current;
    }

    private record LabelToken(String skillId, String label, String lowercaseLabel) {
        static LabelToken of(String skillId, String label) {
            return new LabelToken(skillId, label, label.toLowerCase(Locale.ROOT));
        }
    }

    private record Section(int start, Requirement requirement) { }
}
