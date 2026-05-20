package com.jobskillsmatcher.studentskill.impl;

import com.jobskillsmatcher.studentskill.StudentSkillService;

import com.jobskillsmatcher.context.cache.CacheConfig;
import com.jobskillsmatcher.module.impl.jpa.ModuleSkill;
import com.jobskillsmatcher.module.ModuleSkillRepository;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.model.ProficiencyLevel;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkill;
import com.jobskillsmatcher.studentskill.impl.jpa.StudentSkillId;
import com.jobskillsmatcher.studentskill.StudentSkillRepository;
import com.jobskillsmatcher.studentskill.model.SkillSource;
import com.jobskillsmatcher.studentskill.port.rest.StudentSkillView;
import com.jobskillsmatcher.studentskill.port.rest.UpdateStudentSkillsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentSkillServiceImpl implements StudentSkillService {

    private final StudentSkillRepository studentSkillRepository;
    private final ModuleSkillRepository moduleSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<StudentSkillView> listForStudent(UUID studentId) {
        List<StudentSkill> rows = studentSkillRepository.findAllByIdStudentId(studentId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, Skill> skillsById = loadSkillsById(
                rows.stream().map(r -> r.getId().getSkillId()).toList());
        List<StudentSkillView> out = new ArrayList<>(rows.size());
        for (StudentSkill row : rows) {
            Skill skill = skillsById.get(row.getId().getSkillId());
            if (skill == null) {
                continue;
            }
            out.add(new StudentSkillView(
                    skill.getId(),
                    skill.getPreferredLabel(),
                    row.getProficiency(),
                    row.getSource()
            ));
        }
        out.sort(Comparator.comparing(StudentSkillView::preferredLabel, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.READINESS_SCORE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.JOB_FEED, allEntries = true)
    })
    @Transactional
    public List<StudentSkillView> updateManualSkills(UUID studentId, UpdateStudentSkillsRequest req) {
        Map<String, StudentSkill> existing = indexExisting(studentId);
        Set<String> requestedIds = new HashSet<>();
        for (UpdateStudentSkillsRequest.Item item : req.items()) {
            requestedIds.add(item.skillId());
        }

        Map<String, Skill> skillsById = loadSkillsById(requestedIds.stream().toList());
        for (String requestedId : requestedIds) {
            if (!skillsById.containsKey(requestedId)) {
                throw new UnknownSkillException(requestedId);
            }
        }

        for (UpdateStudentSkillsRequest.Item item : req.items()) {
            StudentSkill row = existing.get(item.skillId());
            if (row == null) {
                row = new StudentSkill();
                row.setId(new StudentSkillId(studentId, item.skillId()));
            }
            row.setProficiency(item.proficiency());
            row.setSource(SkillSource.MANUAL);
            studentSkillRepository.save(row);
        }

        List<StudentSkill> toDelete = new ArrayList<>();
        for (StudentSkill row : existing.values()) {
            if (row.getSource() != SkillSource.MANUAL) {
                continue;
            }
            if (!requestedIds.contains(row.getId().getSkillId())) {
                toDelete.add(row);
            }
        }
        if (!toDelete.isEmpty()) {
            studentSkillRepository.deleteAll(toDelete);
        }

        return listForStudent(studentId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.READINESS_SCORE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.JOB_FEED, allEntries = true)
    })
    @Transactional
    public List<StudentSkillView> applyModules(UUID studentId, List<UUID> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) {
            return listForStudent(studentId);
        }
        List<ModuleSkill> moduleSkills = moduleSkillRepository.findAllByIdModuleIdIn(moduleIds);
        Map<String, ProficiencyLevel> highestByModuleSkill = new HashMap<>();
        for (ModuleSkill link : moduleSkills) {
            String skillId = link.getId().getSkillId();
            highestByModuleSkill.merge(skillId, link.getProficiency(), StudentSkillServiceImpl::max);
        }

        Map<String, StudentSkill> existing = indexExisting(studentId);
        for (Map.Entry<String, ProficiencyLevel> e : highestByModuleSkill.entrySet()) {
            StudentSkill row = existing.get(e.getKey());
            if (row == null) {
                row = new StudentSkill();
                row.setId(new StudentSkillId(studentId, e.getKey()));
                row.setProficiency(e.getValue());
                row.setSource(SkillSource.MODULE);
                studentSkillRepository.save(row);
                continue;
            }
            if (row.getSource() == SkillSource.MANUAL) {
                continue;
            }
            row.setProficiency(max(row.getProficiency(), e.getValue()));
            row.setSource(SkillSource.MODULE);
            studentSkillRepository.save(row);
        }
        return listForStudent(studentId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.READINESS_SCORE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.JOB_FEED, allEntries = true)
    })
    @Transactional
    public void removeSkill(UUID studentId, String skillId) {
        int removed = studentSkillRepository.deleteByStudentIdAndSkillId(studentId, skillId);
        if (removed == 0) {
            throw new StudentSkillNotFoundException(studentId, skillId);
        }
    }

    private Map<String, StudentSkill> indexExisting(UUID studentId) {
        Map<String, StudentSkill> out = new HashMap<>();
        for (StudentSkill row : studentSkillRepository.findAllByIdStudentId(studentId)) {
            out.put(row.getId().getSkillId(), row);
        }
        return out;
    }

    private Map<String, Skill> loadSkillsById(List<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Skill> out = new HashMap<>();
        for (Skill skill : skillRepository.findAllById(ids)) {
            out.put(skill.getId(), skill);
        }
        return out;
    }

    static ProficiencyLevel max(ProficiencyLevel a, ProficiencyLevel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    public static class UnknownSkillException extends RuntimeException {
        public UnknownSkillException(String skillId) {
            super("Unknown skill: " + skillId);
        }
    }

    public static class StudentSkillNotFoundException extends RuntimeException {
        public StudentSkillNotFoundException(UUID studentId, String skillId) {
            super("Student skill not found: " + studentId + " / " + skillId);
        }
    }
}
