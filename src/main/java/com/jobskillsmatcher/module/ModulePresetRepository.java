package com.jobskillsmatcher.module;

import com.jobskillsmatcher.module.impl.jpa.ModulePreset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModulePresetRepository extends JpaRepository<ModulePreset, UUID> {

    Optional<ModulePreset> findByCode(String code);
}
