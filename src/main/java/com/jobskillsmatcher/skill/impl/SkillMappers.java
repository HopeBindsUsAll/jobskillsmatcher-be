package com.jobskillsmatcher.skill.impl;

import com.jobskillsmatcher.skill.SkillMapper;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.port.rest.SkillView;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SkillMappers implements SkillMapper {

    @Override
    public SkillView toView(Skill e) {
        String[] alt = e.getAltLabels();
        return new SkillView(
                e.getId(),
                e.getPreferredLabel(),
                alt == null ? List.of() : Arrays.asList(alt),
                e.getDescription()
        );
    }
}
