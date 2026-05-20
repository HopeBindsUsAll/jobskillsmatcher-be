package com.jobskillsmatcher.recommendation.port.rest;

import com.jobskillsmatcher.context.security.CurrentUser;
import com.jobskillsmatcher.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs/{id}/recommendations")
@RequiredArgsConstructor
public class JobRecommendationsController {

    private final RecommendationService recommendationService;

    @GetMapping
    public JobRecommendationsView forJob(@PathVariable("id") UUID jobId) {
        return recommendationService.forJob(CurrentUser.requireId(), jobId);
    }
}
