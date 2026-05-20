package com.jobskillsmatcher.region;

import com.jobskillsmatcher.region.impl.jpa.RegionRequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegionRequestRepository extends JpaRepository<RegionRequest, UUID> {

    boolean existsByStudentIdAndCountryAndCity(UUID studentId, String country, String city);
}
