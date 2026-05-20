package com.jobskillsmatcher.user.port.rest;

import com.jobskillsmatcher.user.model.Role;

import java.util.UUID;

public record UserView(UUID id, String email, Role role, boolean enabled) {
}
