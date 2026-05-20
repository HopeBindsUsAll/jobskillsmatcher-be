package com.jobskillsmatcher.job.port.rest;

import com.jobskillsmatcher.job.model.Requirement;

public record JobSkillView(
        String skillId,
        String preferredLabel,
        Requirement requirement
) { }
