package com.jobskillsmatcher.skill;

import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.port.rest.SkillView;

public interface SkillMapper {

    SkillView toView(Skill skill);
}
