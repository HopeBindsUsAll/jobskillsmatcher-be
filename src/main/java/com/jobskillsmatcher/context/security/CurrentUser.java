package com.jobskillsmatcher.context.security;

import com.jobskillsmatcher.user.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<UUID> findId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof String subject)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static UUID requireId() {
        return findId().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public static Optional<Role> findRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            String stripped = name.startsWith("ROLE_") ? name.substring(5) : name;
            try {
                return Optional.of(Role.valueOf(stripped));
            } catch (IllegalArgumentException ignored) {
                // skip non-role authorities
            }
        }
        return Optional.empty();
    }

    public static Role requireRole() {
        return findRole().orElseThrow(() -> new IllegalStateException("No role authority"));
    }
}
