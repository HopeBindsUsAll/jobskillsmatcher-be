package com.jobskillsmatcher.user;

import com.jobskillsmatcher.user.impl.jpa.AdminProfile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {
}
