package com.jobskillsmatcher.matching.model;

import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;

public record MatchedSkill(
        String skillId,
        String preferredLabel,
        Requirement requirement,
        ProficiencyLevel proficiency
) { }
