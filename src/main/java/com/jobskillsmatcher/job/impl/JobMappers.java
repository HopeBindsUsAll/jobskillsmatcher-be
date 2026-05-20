package com.jobskillsmatcher.job.impl;

import com.jobskillsmatcher.job.JobMapper;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.port.rest.JobDetailView;
import com.jobskillsmatcher.job.port.rest.JobSkillView;
import com.jobskillsmatcher.job.port.rest.JobSummaryView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobMappers implements JobMapper {

    @Override
    public JobSummaryView toSummary(Job job) {
        return JobSummaryView.from(job);
    }

    @Override
    public JobDetailView toDetail(Job job, List<JobSkillView> required, List<JobSkillView> preferred) {
        return JobDetailView.from(job, required, preferred);
    }
}
