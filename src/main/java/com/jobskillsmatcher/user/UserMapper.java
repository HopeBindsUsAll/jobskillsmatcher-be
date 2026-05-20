package com.jobskillsmatcher.user;

import com.jobskillsmatcher.user.impl.jpa.AdminProfile;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.port.rest.AdminUserView;
import com.jobskillsmatcher.user.port.rest.MeView;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface UserMapper {

    MeView toMeView(User user, StudentProfile studentProfile, AdminProfile adminProfile);

    AdminUserView toAdminUserView(User user, String displayName);

    void update(User user, OAuth2User principal);
}
