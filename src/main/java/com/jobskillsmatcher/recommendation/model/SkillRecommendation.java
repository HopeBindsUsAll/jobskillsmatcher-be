package com.jobskillsmatcher.recommendation.model;

import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.resource.port.rest.ResourceView;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;

import java.util.List;

public record SkillRecommendation(
        String skillId,
        String preferredLabel,
        Requirement requirement,
        RecommendationReason reason,
        ProficiencyLevel currentLevel,
        ProficiencyLevel targetLevel,
        List<ResourceView> resources
) { }
