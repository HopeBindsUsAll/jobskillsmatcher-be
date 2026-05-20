package com.jobskillsmatcher.skill.impl;

import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TfIdfBootstrapRunner implements ApplicationRunner {

    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Skill> skills = skillRepository.findAll();
        if (skills.isEmpty()) {
            log.info("TF-IDF bootstrap skipped: no skills in database yet");
            return;
        }
        boolean allVectored = skills.stream().allMatch(s -> s.getTfidfVector() != null && s.getTfidfVector().length > 0);
        if (allVectored) {
            log.info("TF-IDF bootstrap skipped: all {} skills already vectorised", skills.size());
            return;
        }

        List<String> ids = new ArrayList<>(skills.size());
        List<String> docs = new ArrayList<>(skills.size());
        for (Skill s : skills) {
            ids.add(s.getId());
            docs.add(s.getPreferredLabel() + ". " + s.getDescription());
        }

        TfIdfCalculator.Result result = TfIdfCalculator.compute(ids, docs);
        log.info("TF-IDF bootstrap: vocabulary size = {}, documents = {}", result.vocabulary().size(), ids.size());

        for (Skill s : skills) {
            double[] vec = result.vectorFor(s.getId());
            s.setTfidfVector(vec);
        }
        skillRepository.saveAll(skills);
        log.info("TF-IDF bootstrap: persisted vectors for {} skills", skills.size());
    }
}
