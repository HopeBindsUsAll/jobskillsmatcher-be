package com.jobskillsmatcher.matching;

import com.jobskillsmatcher.matching.model.ScoreBreakdown;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReadinessService {

    ScoreBreakdown score(UUID studentId, UUID jobId);

    Map<UUID, ScoreBreakdown> scoreMany(UUID studentId, List<UUID> jobIds);
}
