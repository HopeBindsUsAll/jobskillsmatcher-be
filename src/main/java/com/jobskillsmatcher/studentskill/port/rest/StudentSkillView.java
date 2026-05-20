package com.jobskillsmatcher.studentskill.port.rest;

import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.model.SkillSource;

public record StudentSkillView(
        String skillId,
        String preferredLabel,
        ProficiencyLevel proficiency,
        SkillSource source
) {
}
