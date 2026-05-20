package com.jobskillsmatcher.skill.impl;

import com.jobskillsmatcher.context.cache.CacheConfig;
import com.jobskillsmatcher.skill.SkillMapper;
import com.jobskillsmatcher.skill.SkillRepository;
import com.jobskillsmatcher.skill.SkillService;
import com.jobskillsmatcher.skill.port.rest.SkillView;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Override
    @Cacheable(value = CacheConfig.ESCO_SKILLS,
            key = "(#query == null ? '' : #query.trim().toLowerCase()) + ':' + (#limit == null ? 0 : #limit)")
    @Transactional(readOnly = true)
    public List<SkillView> search(String query, Integer limit) {
        int effectiveLimit = clamp(limit);
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return skillRepository.findAll(PageRequest.of(0, effectiveLimit))
                    .map(skillMapper::toView)
                    .getContent();
        }
        return skillRepository.searchByTrigram(trimmed, effectiveLimit).stream()
                .map(skillMapper::toView)
                .toList();
    }

    private int clamp(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
