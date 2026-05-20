package com.jobskillsmatcher.auth.impl;

import com.jobskillsmatcher.auth.AuthMapper;
import com.jobskillsmatcher.auth.impl.jpa.Auth;
import com.jobskillsmatcher.user.impl.jpa.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthMappers implements AuthMapper {

    private final EntityManager em;

    @Override
    public Auth fromUser(UUID userId) {
        return fromUser(em.getReference(User.class, userId));
    }

    @Override
    public Auth fromUser(User user) {
        Auth auth = new Auth();
        auth.setUser(user);
        auth.setJti(UUID.randomUUID());
        return auth;
    }
}
