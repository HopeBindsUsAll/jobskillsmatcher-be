package com.jobskillsmatcher.ingest.impl.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JSearchResponse(String status, List<JSearchJob> data) {

    public List<JSearchJob> safeData() {
        return data == null ? List.of() : data;
    }
}
