package com.jobskillsmatcher.studentskill.port.rest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ApplyModulesRequest(@NotNull @NotEmpty List<UUID> moduleIds) {
}
