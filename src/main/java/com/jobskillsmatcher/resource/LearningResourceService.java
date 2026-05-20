package com.jobskillsmatcher.resource;

import com.jobskillsmatcher.resource.model.ResourceType;
import com.jobskillsmatcher.resource.port.rest.ResourceView;
import com.jobskillsmatcher.resource.port.rest.UpsertResourceRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface LearningResourceService {

    Page<ResourceView> list(String query, ResourceType type, int page, int size);

    ResourceView get(UUID id);

    ResourceView create(UpsertResourceRequest req);

    ResourceView update(UUID id, UpsertResourceRequest req);

    void delete(UUID id);
}
