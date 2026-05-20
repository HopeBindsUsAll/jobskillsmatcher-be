package com.jobskillsmatcher.auth;

import com.jobskillsmatcher.auth.impl.jpa.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthRepository extends JpaRepository<Auth, UUID> {

    Optional<Auth> findByIdAndJti(UUID id, UUID jti);
}
