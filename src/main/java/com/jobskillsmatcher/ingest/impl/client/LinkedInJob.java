package com.jobskillsmatcher.ingest.impl.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.OffsetDateTime;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record LinkedInJob(
        String id,
        String title,
        String organization,
        String url,
        @JsonProperty("date_posted")
        @JsonDeserialize(using = LenientOffsetDateTimeDeserializer.class)
        OffsetDateTime datePosted,
        @JsonProperty("description_text") String descriptionText,
        @JsonProperty("location_type") String locationType,
        @JsonProperty("cities_derived") List<String> citiesDerived,
        @JsonProperty("countries_derived") List<String> countriesDerived
) { }
