package com.jobskillsmatcher.matching.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Weighted Jaccard: J = sum(min) / sum(max) over the union of student and job skills.
// Optional penalty subtracts extraSkillPenalty * (student-only skills).
public final class WeightedJaccardCalculator {

    private WeightedJaccardCalculator() { }

    public static double compute(Map<String, Double> studentSkillWeights,
                                 Map<String, Double> jobSkillWeights,
                                 double extraSkillPenalty) {
        if ((studentSkillWeights == null || studentSkillWeights.isEmpty())
                && (jobSkillWeights == null || jobSkillWeights.isEmpty())) {
            return 0.0;
        }
        Set<String> union = new HashSet<>();
        if (studentSkillWeights != null) union.addAll(studentSkillWeights.keySet());
        if (jobSkillWeights != null) union.addAll(jobSkillWeights.keySet());
        double num = 0.0;
        double den = 0.0;
        int extras = 0;
        for (String skill : union) {
            double sw = studentSkillWeights == null ? 0.0 : studentSkillWeights.getOrDefault(skill, 0.0);
            double jw = jobSkillWeights == null ? 0.0 : jobSkillWeights.getOrDefault(skill, 0.0);
            num += Math.min(sw, jw);
            den += Math.max(sw, jw);
            if (jw == 0.0 && sw > 0.0) {
                extras++;
            }
        }
        if (den <= 0.0) {
            return 0.0;
        }
        double j = num / den;
        if (extraSkillPenalty > 0.0 && extras > 0) {
            j = Math.max(0.0, j - extraSkillPenalty * extras);
        }
        return j;
    }

    // Coverage: how much of the job's skill weight the student covers.
    // Sums only over the job's skills, so unrelated student skills don't shrink the score.
    public static double computeCoverage(Map<String, Double> studentSkillWeights,
                                         Map<String, Double> jobSkillWeights) {
        if (jobSkillWeights == null || jobSkillWeights.isEmpty()) {
            return 0.0;
        }
        double num = 0.0;
        double den = 0.0;
        for (Map.Entry<String, Double> e : jobSkillWeights.entrySet()) {
            double jw = e.getValue();
            if (jw <= 0.0) continue;
            double sw = studentSkillWeights == null
                    ? 0.0
                    : studentSkillWeights.getOrDefault(e.getKey(), 0.0);
            num += Math.min(sw, jw);
            den += jw;
        }
        if (den <= 0.0) {
            return 0.0;
        }
        return num / den;
    }
}
