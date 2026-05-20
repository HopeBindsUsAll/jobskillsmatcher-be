package com.jobskillsmatcher.skill;

import com.jobskillsmatcher.skill.port.rest.SkillView;

import java.util.List;

public interface SkillService {

    List<SkillView> search(String query, Integer limit);
}
