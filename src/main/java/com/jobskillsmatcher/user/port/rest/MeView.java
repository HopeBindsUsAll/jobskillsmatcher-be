package com.jobskillsmatcher.user.port.rest;

import com.jobskillsmatcher.user.model.RemotePreference;
import com.jobskillsmatcher.user.model.Role;

import java.util.UUID;

public record MeView(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        Profile profile
) {
    public record Profile(
            String fullName,
            String displayName,
            String preferredRole,
            String country,
            String city,
            RemotePreference remotePreference
    ) {
    }
}
