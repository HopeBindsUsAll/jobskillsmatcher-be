package com.jobskillsmatcher.recommendation.port.rest;

import com.jobskillsmatcher.recommendation.model.SkillRecommendation;

import java.util.List;
import java.util.UUID;

public record JobRecommendationsView(
        UUID jobId,
        List<SkillRecommendation> required,
        List<SkillRecommendation> preferred
) { }
