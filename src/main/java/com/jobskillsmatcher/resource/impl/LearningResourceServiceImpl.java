package com.jobskillsmatcher.resource.impl;

import com.jobskillsmatcher.resource.LearningResourceService;

import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.LearningResourceRepository;
import com.jobskillsmatcher.resource.impl.jpa.ResourceSkill;
import com.jobskillsmatcher.resource.impl.jpa.ResourceSkillId;
import com.jobskillsmatcher.resource.ResourceSkillRepository;
import com.jobskillsmatcher.resource.model.Provider;
import com.jobskillsmatcher.resource.model.ResourceType;
import com.jobskillsmatcher.resource.port.rest.ResourceSkillRef;
import com.jobskillsmatcher.resource.port.rest.ResourceView;
import com.jobskillsmatcher.resource.port.rest.UpsertResourceRequest;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningResourceServiceImpl implements LearningResourceService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LearningResourceRepository resourceRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public Page<ResourceView> list(String query, ResourceType type, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Page<LearningResource> rows = resourceRepository.search(
                query == null ? "" : query.trim(),
                type,
                PageRequest.of(safePage, safeSize));
        Map<UUID, List<ResourceSkillRef>> skillsByResource = loadSkillsForResources(rows.getContent());
        return rows.map(e -> ResourceView.from(e, skillsByResource.getOrDefault(e.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public ResourceView get(UUID id) {
        LearningResource entity = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return ResourceView.from(entity, loadSkills(entity.getId()));
    }

    @Transactional
    public ResourceView create(UpsertResourceRequest req) {
        Set<String> skillIds = validateSkillIds(req.skillIds());
        LearningResource entity = new LearningResource();
        entity.setId(UUID.randomUUID());
        applyRequest(entity, req);
        resourceRepository.save(entity);
        replaceSkills(entity.getId(), skillIds);
        return ResourceView.from(entity, loadSkills(entity.getId()));
    }

    @Transactional
    public ResourceView update(UUID id, UpsertResourceRequest req) {
        Set<String> skillIds = validateSkillIds(req.skillIds());
        LearningResource entity = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        applyRequest(entity, req);
        resourceRepository.save(entity);
        replaceSkills(entity.getId(), skillIds);
        return ResourceView.from(entity, loadSkills(entity.getId()));
    }

    @Transactional
    public void delete(UUID id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        resourceSkillRepository.deleteByResourceId(id);
        resourceRepository.deleteById(id);
    }

    private void applyRequest(LearningResource entity, UpsertResourceRequest req) {
        entity.setType(req.type());
        entity.setDifficulty(req.difficulty());
        entity.setTitle(req.title().trim());
        entity.setDescription(req.description() == null ? "" : req.description().trim());
        entity.setUrl(req.url().trim());
        entity.setProvider(canonicalProvider(req.provider()));
    }

    // Reject unknown providers. Admins must opt in by passing "Other" explicitly.
    private static String canonicalProvider(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return Provider.fromLabel(raw.trim())
                .map(Provider::canonical)
                .orElseThrow(() -> new UnknownResourceProviderException(raw.trim()));
    }

    private Set<String> validateSkillIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<String> dedup = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                dedup.add(id.trim());
            }
        }
        if (dedup.isEmpty()) {
            return Set.of();
        }
        Set<String> known = new HashSet<>();
        for (Skill s : skillRepository.findAllById(dedup)) {
            known.add(s.getId());
        }
        for (String id : dedup) {
            if (!known.contains(id)) {
                throw new UnknownResourceSkillException(id);
            }
        }
        return dedup;
    }

    private void replaceSkills(UUID resourceId, Set<String> skillIds) {
        resourceSkillRepository.deleteByResourceId(resourceId);
        if (skillIds.isEmpty()) {
            return;
        }
        List<ResourceSkill> rows = new ArrayList<>(skillIds.size());
        for (String skillId : skillIds) {
            rows.add(new ResourceSkill(new ResourceSkillId(resourceId, skillId)));
        }
        resourceSkillRepository.saveAll(rows);
    }

    private List<ResourceSkillRef> loadSkills(UUID resourceId) {
        List<ResourceSkill> rows = resourceSkillRepository.findAllByIdResourceId(resourceId);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> skillIds = rows.stream().map(rs -> rs.getId().getSkillId()).toList();
        Map<String, String> labelById = new HashMap<>();
        for (Skill s : skillRepository.findAllById(skillIds)) {
            labelById.put(s.getId(), s.getPreferredLabel());
        }
        List<ResourceSkillRef> out = new ArrayList<>(skillIds.size());
        for (String id : skillIds) {
            out.add(new ResourceSkillRef(id, labelById.getOrDefault(id, id)));
        }
        out.sort(Comparator.comparing(ResourceSkillRef::preferredLabel, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private Map<UUID, List<ResourceSkillRef>> loadSkillsForResources(List<LearningResource> resources) {
        if (resources.isEmpty()) return Map.of();
        List<UUID> ids = resources.stream().map(LearningResource::getId).toList();
        List<ResourceSkill> links = resourceSkillRepository.findAllByIdResourceIdIn(ids);
        if (links.isEmpty()) return Map.of();
        Set<String> skillIds = new HashSet<>();
        for (ResourceSkill l : links) skillIds.add(l.getId().getSkillId());
        Map<String, String> labelById = new HashMap<>();
        for (Skill s : skillRepository.findAllById(skillIds)) {
            labelById.put(s.getId(), s.getPreferredLabel());
        }
        Map<UUID, List<ResourceSkillRef>> out = new HashMap<>();
        for (ResourceSkill l : links) {
            String skillId = l.getId().getSkillId();
            out.computeIfAbsent(l.getId().getResourceId(), k -> new ArrayList<>())
                    .add(new ResourceSkillRef(skillId, labelById.getOrDefault(skillId, skillId)));
        }
        for (List<ResourceSkillRef> list : out.values()) {
            list.sort(Comparator.comparing(ResourceSkillRef::preferredLabel, String.CASE_INSENSITIVE_ORDER));
        }
        return out;
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(UUID id) {
            super("Learning resource not found: " + id);
        }
    }

    public static class UnknownResourceSkillException extends RuntimeException {
        public UnknownResourceSkillException(String skillId) {
            super("Unknown skill: " + skillId);
        }
    }

    public static class UnknownResourceProviderException extends RuntimeException {
        public UnknownResourceProviderException(String provider) {
            super("Provider not in allowlist: " + provider
                    + ". Use one of the canonical Provider names or \"Other\".");
        }
    }
}
