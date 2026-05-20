package com.jobskillsmatcher.job;

import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.port.rest.JobDetailView;
import com.jobskillsmatcher.job.port.rest.JobSkillView;
import com.jobskillsmatcher.job.port.rest.JobSummaryView;

import java.util.List;

public interface JobMapper {

    JobSummaryView toSummary(Job job);

    JobDetailView toDetail(Job job, List<JobSkillView> required, List<JobSkillView> preferred);
}
