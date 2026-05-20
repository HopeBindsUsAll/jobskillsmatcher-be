package com.jobskillsmatcher.region.impl;

import com.jobskillsmatcher.region.RegionRequestMapper;
import com.jobskillsmatcher.region.RegionRequestRepository;
import com.jobskillsmatcher.region.RegionRequestService;
import com.jobskillsmatcher.region.impl.jpa.RegionRequest;
import com.jobskillsmatcher.region.port.rest.RegionRequestView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionRequestServiceImpl implements RegionRequestService {

    private final RegionRequestRepository repository;
    private final RegionRequestMapper mapper;

    @Override
    @Transactional
    public RegionRequestView create(UUID studentId, String country, String city) {
        String normCountry = country == null ? "" : country.trim().toUpperCase(Locale.ROOT);
        if (normCountry.length() > 2) normCountry = normCountry.substring(0, 2);
        String normCity = city == null ? "" : city.trim();
        if (normCity.length() > 255) normCity = normCity.substring(0, 255);

        if (repository.existsByStudentIdAndCountryAndCity(studentId, normCountry, normCity)) {
            RegionRequest placeholder = new RegionRequest();
            placeholder.setStudentId(studentId);
            placeholder.setCountry(normCountry);
            placeholder.setCity(normCity);
            return mapper.toView(placeholder);
        }

        RegionRequest entity = new RegionRequest();
        entity.setStudentId(studentId);
        entity.setCountry(normCountry);
        entity.setCity(normCity);
        repository.save(entity);
        return mapper.toView(entity);
    }
}
