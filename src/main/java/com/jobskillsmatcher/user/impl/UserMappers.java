package com.jobskillsmatcher.user.impl;

import com.jobskillsmatcher.user.UserMapper;
import com.jobskillsmatcher.user.impl.jpa.AdminProfile;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.model.Role;
import com.jobskillsmatcher.user.port.rest.AdminUserView;
import com.jobskillsmatcher.user.port.rest.MeView;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class UserMappers implements UserMapper {

    @Override
    public MeView toMeView(User user, StudentProfile studentProfile, AdminProfile adminProfile) {
        if (user.getRole() == Role.ADMIN) {
            return new MeView(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(),
                    new MeView.Profile(null, adminProfile == null ? null : adminProfile.getDisplayName(),
                            null, null, null, null));
        }
        MeView.Profile profile = studentProfile == null
                ? new MeView.Profile(null, null, null, null, null, null)
                : new MeView.Profile(studentProfile.getFullName(), null, studentProfile.getPreferredRole(),
                studentProfile.getCountry(), studentProfile.getCity(), studentProfile.getRemotePreference());
        return new MeView(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(), profile);
    }

    @Override
    public AdminUserView toAdminUserView(User user, String displayName) {
        return new AdminUserView(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(),
                displayName, user.getCreatedAt(), user.getUpdatedAt());
    }

    @Override
    public void update(User user, OAuth2User principal) {
        String email = principal.getAttribute("email");
        String googleSub = principal.getAttribute("sub");
        if (email != null && user.getEmail() == null) {
            user.setEmail(email.toLowerCase());
        }
        if (googleSub != null && user.getGoogleSub() == null) {
            user.setGoogleSub(googleSub);
        }
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
            user.setEnabled(true);
        }
    }
}
