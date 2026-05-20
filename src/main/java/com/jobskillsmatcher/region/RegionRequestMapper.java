package com.jobskillsmatcher.region;

import com.jobskillsmatcher.region.impl.jpa.RegionRequest;
import com.jobskillsmatcher.region.port.rest.RegionRequestView;

public interface RegionRequestMapper {

    RegionRequestView toView(RegionRequest entity);
}
