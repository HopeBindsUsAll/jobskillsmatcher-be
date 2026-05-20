package com.jobskillsmatcher.matching;

import com.jobskillsmatcher.job.model.Seniority;
import com.jobskillsmatcher.matching.port.rest.JobFeedItemView;
import com.jobskillsmatcher.matching.port.rest.JobScoredDetailView;
import com.jobskillsmatcher.matching.port.rest.ReadinessHeadlineView;
import com.jobskillsmatcher.matching.port.rest.ReadinessTrendView;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface JobFeedService {

    Page<JobFeedItemView> feed(UUID studentId,
                               String country,
                               String city,
                               boolean remoteOnly,
                               Seniority seniority,
                               String search,
                               int page,
                               int size);

    JobScoredDetailView detail(UUID studentId, UUID jobId);

    ReadinessHeadlineView headline(UUID studentId);

    ReadinessTrendView trend(UUID studentId);

    List<UUID> snapshotTopJobIds(UUID studentId, String country, int limit);
}
