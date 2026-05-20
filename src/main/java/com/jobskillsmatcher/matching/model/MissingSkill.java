package com.jobskillsmatcher.matching.model;

import com.jobskillsmatcher.job.model.Requirement;

public record MissingSkill(
        String skillId,
        String preferredLabel,
        Requirement requirement
) { }
