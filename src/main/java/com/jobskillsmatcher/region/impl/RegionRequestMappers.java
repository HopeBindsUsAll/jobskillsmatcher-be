package com.jobskillsmatcher.region.impl;

import com.jobskillsmatcher.region.RegionRequestMapper;
import com.jobskillsmatcher.region.impl.jpa.RegionRequest;
import com.jobskillsmatcher.region.port.rest.RegionRequestView;
import org.springframework.stereotype.Component;

@Component
public class RegionRequestMappers implements RegionRequestMapper {

    @Override
    public RegionRequestView toView(RegionRequest entity) {
        return RegionRequestView.from(entity);
    }
}
