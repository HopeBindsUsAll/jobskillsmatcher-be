package com.jobskillsmatcher.region.port.rest;

import com.jobskillsmatcher.region.impl.jpa.RegionRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegionRequestView(
        UUID id,
        String country,
        String city,
        OffsetDateTime requestedAt
) {
    public static RegionRequestView from(RegionRequest e) {
        return new RegionRequestView(e.getId(), e.getCountry(), e.getCity(), e.getRequestedAt());
    }
}
