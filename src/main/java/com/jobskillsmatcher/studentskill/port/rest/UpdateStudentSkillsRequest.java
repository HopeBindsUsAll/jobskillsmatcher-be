package com.jobskillsmatcher.studentskill.port.rest;

import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateStudentSkillsRequest(@Valid @NotNull List<Item> items) {
    public record Item(
            @NotBlank String skillId,
            @NotNull ProficiencyLevel proficiency
    ) {
    }
}
