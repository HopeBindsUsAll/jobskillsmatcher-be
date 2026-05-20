package com.jobskillsmatcher.user.port.rest;

import com.jobskillsmatcher.user.model.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserView(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        String displayName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
