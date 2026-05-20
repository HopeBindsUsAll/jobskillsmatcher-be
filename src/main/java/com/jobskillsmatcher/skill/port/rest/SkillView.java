package com.jobskillsmatcher.skill.port.rest;

import java.util.List;

public record SkillView(
        String id,
        String preferredLabel,
        List<String> altLabels,
        String description
) {
}
