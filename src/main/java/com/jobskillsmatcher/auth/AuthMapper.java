package com.jobskillsmatcher.auth;

import com.jobskillsmatcher.auth.impl.jpa.Auth;
import com.jobskillsmatcher.user.impl.jpa.User;

import java.util.UUID;

public interface AuthMapper {

    Auth fromUser(UUID userId);

    Auth fromUser(User user);
}
