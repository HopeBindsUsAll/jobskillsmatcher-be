package com.jobskillsmatcher.user.port.rest;

import com.jobskillsmatcher.user.model.RemotePreference;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 200) String fullName,
        @Size(max = 120) String preferredRole,
        @Size(min = 2, max = 2) String country,
        @Size(max = 120) String city,
        RemotePreference remotePreference
) {
}
