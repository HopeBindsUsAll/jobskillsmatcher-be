package com.jobskillsmatcher.ingest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.ingest.IngestRunRepository;
import com.jobskillsmatcher.ingest.IngestScheduleRepository;
import com.jobskillsmatcher.ingest.impl.client.JSearchClient;
import com.jobskillsmatcher.ingest.impl.client.JSearchJob;
import com.jobskillsmatcher.ingest.impl.client.LinkedInJobClient;
import com.jobskillsmatcher.ingest.impl.groq.GroqSkillEnricher;
import com.jobskillsmatcher.ingest.impl.jpa.IngestRun;
import com.jobskillsmatcher.ingest.model.IngestRequest;
import com.jobskillsmatcher.ingest.model.RunStatus;
import com.jobskillsmatcher.job.JobRepository;
import com.jobskillsmatcher.job.JobSkillRepository;
import com.jobskillsmatcher.job.impl.jpa.Job;
import com.jobskillsmatcher.job.impl.jpa.JobSkill;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.job.model.Seniority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestServiceImplTest {

    @Mock
    JSearchClient jsearchClient;

    @Mock
    LinkedInJobClient linkedInJobClient;

    @Mock
    JobSkillExtractor jobSkillExtractor;

    @Mock
    GroqSkillEnricher groqSkillEnricher;

    @Mock
    JobRepository jobRepository;

    @Mock
    JobSkillRepository jobSkillRepository;

    @Mock
    IngestRunRepository ingestRunRepository;

    @Mock
    IngestScheduleRepository ingestScheduleRepository;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    IngestServiceImpl ingestService;

    @Test
    void successRunOnceCreatesIngestRunAndStoresJobs() throws Exception {
        JSearchJob raw = jobBuilder("ext-1", "Java Backend Engineer");
        when(jsearchClient.search(eq("java"), eq("US"), anyInt(), anyInt())).thenReturn(List.of(raw));
        when(jobRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(jobSkillExtractor.extract(anyString())).thenReturn(Map.of("esco/java", Requirement.REQUIRED));
        when(groqSkillEnricher.enrich(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        IngestRun run = ingestService.runOnce(IngestRequest.manual("java", "US", "", false, false));

        assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
        assertThat(run.getFetchedCount()).isEqualTo(1);
        assertThat(run.getStoredCount()).isEqualTo(1);
        assertThat(run.getFinishedAt()).isNotNull();
        verify(jobRepository).save(any(Job.class));
        verify(jobSkillRepository).save(any(JobSkill.class));
        verify(ingestRunRepository, atLeastOnce()).save(run);
    }

    @Test
    void successSkipsJobsWithBlankJobIds() {
        JSearchJob blank = jobBuilder("", "Junior Dev");
        JSearchJob nullId = jobBuilder(null, "Mid Dev");
        when(jsearchClient.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(blank, nullId));

        IngestRun run = ingestService.runOnce(IngestRequest.manual("dev", "US", "", false, false));

        assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
        assertThat(run.getFetchedCount()).isEqualTo(2);
        assertThat(run.getStoredCount()).isZero();
        verify(jobRepository, times(0)).save(any(Job.class));
    }

    @Test
    void failedRunOnceMarksFAILEDAndTruncatesError() {
        when(jsearchClient.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("x".repeat(5000)));

        IngestRun run = ingestService.runOnce(IngestRequest.manual("q", "US", "", false, false));

        assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED);
        assertThat(run.getError()).hasSize(4000);
        verify(ingestRunRepository, atLeastOnce()).save(run);
    }

    @Test
    void coveragePerJobErrorDoesNotRollbackBatch() throws Exception {
        JSearchJob badJob = jobBuilder("ext-bad", "Bad");
        JSearchJob goodJob = jobBuilder("ext-good", "Good");
        when(jsearchClient.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(badJob, goodJob));
        when(jobRepository.findByExternalId("ext-bad"))
                .thenThrow(new RuntimeException("DB blew up on bad job"));
        when(jobRepository.findByExternalId("ext-good")).thenReturn(Optional.empty());
        when(jobSkillExtractor.extract(anyString())).thenReturn(Map.of());
        when(groqSkillEnricher.enrich(anyString(), any())).thenReturn(Map.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        IngestRun run = ingestService.runOnce(IngestRequest.manual("q", "US", "", false, false));

        assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS);
        assertThat(run.getStoredCount()).isEqualTo(1);
    }

    @Test
    void successStoreJobCreatesNewAndReplacesSkills() throws Exception {
        JSearchJob raw = jobBuilder("ext-1", "Backend Engineer");
        when(jobRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(jobSkillExtractor.extract(anyString())).thenReturn(Map.of("esco/java", Requirement.REQUIRED));
        when(groqSkillEnricher.enrich(anyString(), any()))
                .thenReturn(Map.of("esco/java", Requirement.REQUIRED,
                        "esco/sql", Requirement.PREFERRED));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        boolean created = ingestService.storeJob(raw,
                IngestRequest.manual("q", "US", "", false, false));

        assertThat(created).isTrue();
        verify(jobSkillRepository).deleteByIdJobId(any());
        verify(jobSkillRepository, times(2)).save(any(JobSkill.class));
    }

    @Test
    void successCountsOnlyNewJobsInStoredCount() throws Exception {
        JSearchJob existingJob = jobBuilder("ext-old", "Senior Backend");
        JSearchJob newJob = jobBuilder("ext-new", "Junior Backend");
        when(jsearchClient.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(existingJob, newJob));
        Job preexisting = new Job();
        preexisting.setExternalId("ext-old");
        when(jobRepository.findByExternalId("ext-old")).thenReturn(Optional.of(preexisting));
        when(jobRepository.findByExternalId("ext-new")).thenReturn(Optional.empty());
        when(jobSkillExtractor.extract(anyString())).thenReturn(Map.of());
        when(groqSkillEnricher.enrich(anyString(), any())).thenReturn(Map.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        IngestRun run = ingestService.runOnce(IngestRequest.manual("q", "US", "", false, false));

        assertThat(run.getFetchedCount()).isEqualTo(2);
        assertThat(run.getStoredCount()).isEqualTo(1);
    }

    @Test
    void successGuessSeniorityFromTitle() throws Exception {
        when(jobRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(jobSkillExtractor.extract(anyString())).thenReturn(Map.of());
        when(groqSkillEnricher.enrich(anyString(), any())).thenReturn(Map.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertThat(savedSeniorityFor("Software Engineering Intern")).isEqualTo(Seniority.INTERN);
        assertThat(savedSeniorityFor("Junior Java Developer")).isEqualTo(Seniority.JUNIOR);
        assertThat(savedSeniorityFor("Senior Backend Engineer")).isEqualTo(Seniority.SENIOR);
        assertThat(savedSeniorityFor("Lead Software Architect")).isEqualTo(Seniority.LEAD);
        assertThat(savedSeniorityFor("Software Engineer")).isEqualTo(Seniority.UNKNOWN);
    }

    @Test
    void successNormalisesCountryAndCurrency() throws Exception {
        JSearchJob raw = new JSearchJob("ext-x", "Engineer", "Acme", "desc",
                null, "Berlin", false, "https://apply", null,
                null, null, "usd-too-long", "monthly");
        when(jobRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(jobSkillExtractor.extract(anyString())).thenReturn(Map.of());
        when(groqSkillEnricher.enrich(anyString(), any())).thenReturn(Map.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ingestService.storeJob(raw, IngestRequest.manual("q", "us", "", false, false));

        ArgumentCaptor<Job> jobCap = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCap.capture());
        Job saved = jobCap.getValue();
        assertThat(saved.getCountry()).isEqualTo("US");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getSalaryPeriod()).isEqualTo("MONTHLY");
    }

    private Seniority savedSeniorityFor(String title) throws Exception {
        JSearchJob raw = jobBuilder("ext-" + title.hashCode(), title);
        ingestService.storeJob(raw, IngestRequest.manual("q", "US", "", false, false));
        ArgumentCaptor<Job> jobCap = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(jobCap.capture());
        return jobCap.getValue().getSeniority();
    }

    private static JSearchJob jobBuilder(String jobId, String title) {
        return new JSearchJob(jobId, title, "Acme Corp", "Description for " + title,
                "US", "Remote", true, "https://apply",
                null, null, null, "USD", "yearly");
    }
}
