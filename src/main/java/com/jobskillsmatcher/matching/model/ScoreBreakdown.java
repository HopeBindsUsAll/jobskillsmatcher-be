package com.jobskillsmatcher.matching.model;

import java.util.List;

public record ScoreBreakdown(
        double jaccard,
        double cosine,
        double score,
        List<MatchedSkill> matchedSkills,
        List<MissingSkill> missingRequired,
        List<MissingSkill> missingPreferred
) { }
