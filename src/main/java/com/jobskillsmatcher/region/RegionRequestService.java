package com.jobskillsmatcher.region;

import com.jobskillsmatcher.region.port.rest.RegionRequestView;

import java.util.UUID;

public interface RegionRequestService {

    RegionRequestView create(UUID studentId, String country, String city);
}
