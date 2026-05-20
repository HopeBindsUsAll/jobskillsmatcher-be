package com.jobskillsmatcher.recommendation.model;

public enum RecommendationReason {
    /** Student does not yet have this skill. */
    MISSING,
    /** Student has the skill, but at a proficiency below the threshold the job demands. */
    UNDER_LEVELED
}
