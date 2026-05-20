package com.jobskillsmatcher.region.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.ingest.IngestService;
import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;
import com.jobskillsmatcher.ingest.model.IngestRequest;
import com.jobskillsmatcher.region.RegionRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/region-requests")
@RequiredArgsConstructor
@Tag(name = "Region Requests",
        description = "Lets students flag interest in a country/city the feed has not covered yet.")
public class RegionRequestController {

    private final RegionRequestService service;
    private final IngestService ingestService;

    @PostMapping
    @Operation(summary = "Request job coverage for a region",
            description = "Submitted from the empty-state CTA when no postings match the active filter. "
                    + "Repeat submissions for the same region are deduplicated per student.")
    @ResponseStatus(HttpStatus.CREATED)
    public RegionRequestView create(@Valid @RequestBody Body body) {
        return service.create(CurrentUser.requireId(), body.country(), body.city());
    }

    @PostMapping("/fulfil")
    @Operation(summary = "Request job coverage and fetch it immediately",
            description = "Records the region interest (deduplicated, same as the plain request) and "
                    + "synchronously pulls jobs from JSearch for the given query + country so the "
                    + "student sees results right away instead of waiting for the overnight ingest.")
    @ResponseStatus(HttpStatus.OK)
    public FulfilResultView fulfil(@Valid @RequestBody FulfilBody body) {
        String country = body.country() == null
                ? ""
                : body.country().trim().toUpperCase(Locale.ROOT);

        RegionRequestView region = service.create(CurrentUser.requireId(), country, body.city());

        IngestRun run = ingestService.runOnce(
                IngestRequest.manual(body.query().trim(), country, "", false, false));

        return new FulfilResultView(
                region,
                run.getFetchedCount(),
                run.getStoredCount(),
                run.getStatus().name());
    }

    public record Body(
            @Size(max = 2) String country,
            @Size(max = 255) String city
    ) { }

    public record FulfilBody(
            @NotBlank @Size(max = 500) String query,
            @Size(max = 2) String country,
            @Size(max = 255) String city
    ) { }

    public record FulfilResultView(
            RegionRequestView region,
            int fetched,
            int stored,
            String status
    ) { }
}
