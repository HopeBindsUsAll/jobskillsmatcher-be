package com.jobskillsmatcher.recommendation;

import com.jobskillsmatcher.recommendation.port.rest.JobRecommendationsView;

import java.util.UUID;

public interface RecommendationService {

    JobRecommendationsView forJob(UUID studentId, UUID jobId);
}
