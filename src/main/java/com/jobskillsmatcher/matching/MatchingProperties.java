package com.jobskillsmatcher.matching;

import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties(prefix = "jobskillsmatcher.matching")
public class MatchingProperties {

    // final = alpha * jaccard + (1 - alpha) * cosine
    private double alpha = 0.6;

    private Map<ProficiencyLevel, Double> proficiencyWeights = defaultProficiencyWeights();

    private Map<Requirement, Double> requirementWeights = defaultRequirementWeights();

    // Penalty per student-only skill, 0 disables.
    private double extraSkillPenalty = 0.0;

    // Coverage mode scores against the job's skills only — unrelated student skills don't hurt.
    private boolean coverageMode = true;

    private static Map<ProficiencyLevel, Double> defaultProficiencyWeights() {
        EnumMap<ProficiencyLevel, Double> m = new EnumMap<>(ProficiencyLevel.class);
        m.put(ProficiencyLevel.BEGINNER, 0.33);
        m.put(ProficiencyLevel.INTERMEDIATE, 0.66);
        m.put(ProficiencyLevel.ADVANCED, 0.85);
        m.put(ProficiencyLevel.EXPERT, 1.0);
        return m;
    }

    private static Map<Requirement, Double> defaultRequirementWeights() {
        EnumMap<Requirement, Double> m = new EnumMap<>(Requirement.class);
        m.put(Requirement.REQUIRED, 1.0);
        m.put(Requirement.PREFERRED, 0.5);
        return m;
    }

    public double getAlpha() { return alpha; }
    public void setAlpha(double alpha) { this.alpha = alpha; }

    public Map<ProficiencyLevel, Double> getProficiencyWeights() { return proficiencyWeights; }
    public void setProficiencyWeights(Map<ProficiencyLevel, Double> w) {
        this.proficiencyWeights = w == null || w.isEmpty() ? defaultProficiencyWeights() : new EnumMap<>(w);
    }

    public Map<Requirement, Double> getRequirementWeights() { return requirementWeights; }
    public void setRequirementWeights(Map<Requirement, Double> w) {
        this.requirementWeights = w == null || w.isEmpty() ? defaultRequirementWeights() : new EnumMap<>(w);
    }

    public double getExtraSkillPenalty() { return extraSkillPenalty; }
    public void setExtraSkillPenalty(double v) { this.extraSkillPenalty = v; }

    public boolean isCoverageMode() { return coverageMode; }
    public void setCoverageMode(boolean v) { this.coverageMode = v; }

    public double weightFor(ProficiencyLevel p) {
        Double v = proficiencyWeights.get(p);
        return v == null ? 0.0 : v;
    }

    public double weightFor(Requirement r) {
        Double v = requirementWeights.get(r);
        return v == null ? 0.0 : v;
    }
}
