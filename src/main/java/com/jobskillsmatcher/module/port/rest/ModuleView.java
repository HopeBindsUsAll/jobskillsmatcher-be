package com.jobskillsmatcher.module.port.rest;

import com.jobskillsmatcher.skill.model.ProficiencyLevel;

import java.util.List;
import java.util.UUID;

public record ModuleView(
        UUID id,
        String code,
        String name,
        String description,
        List<Skill> skills
) {
    public record Skill(String skillId, String preferredLabel, ProficiencyLevel proficiency) {
    }
}
