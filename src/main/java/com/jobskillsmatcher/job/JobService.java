package com.jobskillsmatcher.job;

import com.jobskillsmatcher.job.port.rest.JobDetailView;
import com.jobskillsmatcher.job.port.rest.JobSummaryView;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface JobService {

    Page<JobSummaryView> list(String country, String city, boolean remoteOnly, int page, int size);

    JobDetailView get(UUID id);

    void delete(UUID id);

    // Bulk delete; missing ids are skipped. Returns the count actually deleted.
    int deleteAll(List<UUID> ids);
}
