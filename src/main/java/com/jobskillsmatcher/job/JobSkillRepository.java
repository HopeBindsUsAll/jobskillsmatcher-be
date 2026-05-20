package com.jobskillsmatcher.job;

import com.jobskillsmatcher.job.impl.jpa.JobSkillId;

import com.jobskillsmatcher.job.impl.jpa.JobSkill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {

    List<JobSkill> findAllByIdJobId(UUID jobId);

    List<JobSkill> findAllByIdJobIdIn(List<UUID> jobIds);

    long deleteByIdJobId(UUID jobId);

    long deleteByIdJobIdIn(List<UUID> jobIds);
}
