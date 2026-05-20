package com.jobskillsmatcher.resource.impl;

import com.jobskillsmatcher.resource.LearningResourceMapper;
import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.port.rest.ResourceSkillRef;
import com.jobskillsmatcher.resource.port.rest.ResourceView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningResourceMappers implements LearningResourceMapper {

    @Override
    public ResourceView toView(LearningResource entity, List<ResourceSkillRef> skills) {
        return ResourceView.from(entity, skills);
    }
}
