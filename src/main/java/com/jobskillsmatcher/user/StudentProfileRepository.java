package com.jobskillsmatcher.user;

import com.jobskillsmatcher.user.impl.jpa.StudentProfile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
}
