package com.jobskillsmatcher.user;

import com.jobskillsmatcher.user.port.rest.AdminUserPatchRequest;
import com.jobskillsmatcher.user.port.rest.AdminUserView;
import com.jobskillsmatcher.user.port.rest.MeView;
import com.jobskillsmatcher.user.port.rest.UpdateProfileRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

public interface UserService {

    MeView loadMe(UUID userId);

    MeView updateStudentProfile(UUID userId, UpdateProfileRequest req);

    Page<AdminUserView> listUsers(com.jobskillsmatcher.user.model.Role role, Boolean enabled, String query, int page, int size);

    AdminUserView patchUser(UUID userId, AdminUserPatchRequest req);

    UUID importByOAuth2(OAuth2User principal);
}
