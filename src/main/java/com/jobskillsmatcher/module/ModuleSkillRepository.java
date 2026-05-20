package com.jobskillsmatcher.module;

import com.jobskillsmatcher.module.impl.jpa.ModuleSkillId;

import com.jobskillsmatcher.module.impl.jpa.ModuleSkill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModuleSkillRepository extends JpaRepository<ModuleSkill, ModuleSkillId> {

    List<ModuleSkill> findAllByIdModuleId(UUID moduleId);

    List<ModuleSkill> findAllByIdModuleIdIn(List<UUID> moduleIds);
}
