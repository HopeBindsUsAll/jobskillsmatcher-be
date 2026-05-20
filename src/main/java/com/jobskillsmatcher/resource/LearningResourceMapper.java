package com.jobskillsmatcher.resource;

import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.port.rest.ResourceSkillRef;
import com.jobskillsmatcher.resource.port.rest.ResourceView;

import java.util.List;

public interface LearningResourceMapper {

    ResourceView toView(LearningResource entity, List<ResourceSkillRef> skills);
}
