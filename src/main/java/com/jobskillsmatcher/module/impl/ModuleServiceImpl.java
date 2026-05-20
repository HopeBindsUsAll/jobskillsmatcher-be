package com.jobskillsmatcher.module.impl;

import com.jobskillsmatcher.module.ModuleService;

import com.jobskillsmatcher.module.impl.jpa.ModulePreset;
import com.jobskillsmatcher.module.ModulePresetRepository;
import com.jobskillsmatcher.module.impl.jpa.ModuleSkill;
import com.jobskillsmatcher.module.ModuleSkillRepository;
import com.jobskillsmatcher.module.port.rest.ModuleView;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModulePresetRepository modulePresetRepository;
    private final ModuleSkillRepository moduleSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<ModuleView> listAll() {
        List<ModulePreset> presets = modulePresetRepository.findAll();
        if (presets.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = presets.stream().map(ModulePreset::getId).toList();
        List<ModuleSkill> links = moduleSkillRepository.findAllByIdModuleIdIn(ids);
        List<String> skillIds = links.stream().map(l -> l.getId().getSkillId()).distinct().toList();
        Map<String, Skill> skillsById = new HashMap<>();
        for (Skill skill : skillRepository.findAllById(skillIds)) {
            skillsById.put(skill.getId(), skill);
        }

        Map<UUID, List<ModuleSkill>> linksByModule = new HashMap<>();
        for (ModuleSkill link : links) {
            linksByModule.computeIfAbsent(link.getId().getModuleId(), k -> new ArrayList<>()).add(link);
        }

        List<ModuleView> out = new ArrayList<>(presets.size());
        for (ModulePreset preset : presets) {
            List<ModuleSkill> mod = linksByModule.getOrDefault(preset.getId(), List.of());
            List<ModuleView.Skill> skillViews = new ArrayList<>(mod.size());
            for (ModuleSkill link : mod) {
                Skill skill = skillsById.get(link.getId().getSkillId());
                if (skill == null) {
                    continue;
                }
                skillViews.add(new ModuleView.Skill(skill.getId(), skill.getPreferredLabel(), link.getProficiency()));
            }
            skillViews.sort(Comparator.comparing(ModuleView.Skill::preferredLabel, String.CASE_INSENSITIVE_ORDER));
            out.add(new ModuleView(
                    preset.getId(),
                    preset.getCode(),
                    preset.getName(),
                    preset.getDescription(),
                    skillViews
            ));
        }
        out.sort(Comparator.comparing(ModuleView::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }
}
