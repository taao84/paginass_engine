package com.paginass.service;

import com.paginass.entity.JobRunStatus;
import com.paginass.entity.JobRunTrigger;
import com.paginass.job.JobService;
import com.paginass.repository.JobRunRepository;
import com.paginass.config.FeatureKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paginass.entity.JobRun;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSchedulerServiceTest {

    @Mock private JobService jobA;
    @Mock private JobService jobB;
    @Mock private JobRunRepository jobRunRepository;
    @Mock private AppFeatureService appFeatureService;

    private JobSchedulerService scheduler;

    // --- runDailyJobs / triggerManually share the same executeJobs path ---

    @Test
    void triggerManually_invokesAllJobs() {
        when(jobA.jobName()).thenReturn("JobA");
        when(jobB.jobName()).thenReturn("JobB");
        when(jobRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        scheduler = new JobSchedulerService(List.of(jobA, jobB), jobRunRepository, appFeatureService);

        scheduler.triggerManually();

        verify(jobA).execute();
        verify(jobB).execute();
    }

    @Test
    void triggerManually_oneJobFails_remainingJobsStillExecute() {
        when(jobA.jobName()).thenReturn("JobA");
        when(jobB.jobName()).thenReturn("JobB");
        when(jobRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Simulated failure")).when(jobA).execute();
        scheduler = new JobSchedulerService(List.of(jobA, jobB), jobRunRepository, appFeatureService);

        scheduler.triggerManually();

        verify(jobA).execute();
        verify(jobB).execute();
    }

    @Test
    void triggerManually_recordsSuccessfulRun() {
        when(jobA.jobName()).thenReturn("JobA");
        when(jobRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        scheduler = new JobSchedulerService(List.of(jobA), jobRunRepository, appFeatureService);

        scheduler.triggerManually();

        ArgumentCaptor<JobRun> captor = ArgumentCaptor.forClass(JobRun.class);
        verify(jobRunRepository).save(captor.capture());
        JobRun saved = captor.getValue();
        assertThat(saved.getJobName()).isEqualTo("JobA");
        assertThat(saved.getTriggeredBy()).isEqualTo(JobRunTrigger.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(JobRunStatus.SUCCESS);
        assertThat(saved.getStartedAt()).isNotNull();
        assertThat(saved.getFinishedAt()).isNotNull();
        assertThat(saved.getMessage()).isNull();
    }

    @Test
    void triggerManually_recordsFailedRun() {
        when(jobA.jobName()).thenReturn("JobA");
        when(jobRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("boom")).when(jobA).execute();
        scheduler = new JobSchedulerService(List.of(jobA), jobRunRepository, appFeatureService);

        scheduler.triggerManually();

        ArgumentCaptor<JobRun> captor = ArgumentCaptor.forClass(JobRun.class);
        verify(jobRunRepository).save(captor.capture());
        JobRun saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobRunStatus.FAILED);
        assertThat(saved.getMessage()).isEqualTo("boom");
        assertThat(saved.getFinishedAt()).isNotNull();
    }

    @Test
    void runDailyJobs_schedulerEnabled_recordsTriggerAsScheduler() {
        when(appFeatureService.isEnabled(FeatureKey.SCHEDULER)).thenReturn(true);
        when(jobA.jobName()).thenReturn("JobA");
        when(jobRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        scheduler = new JobSchedulerService(List.of(jobA), jobRunRepository, appFeatureService);

        scheduler.runDailyJobs();

        ArgumentCaptor<JobRun> captor = ArgumentCaptor.forClass(JobRun.class);
        verify(jobRunRepository).save(captor.capture());
        assertThat(captor.getValue().getTriggeredBy()).isEqualTo(JobRunTrigger.SCHEDULER);
    }

    @Test
    void runDailyJobs_schedulerDisabled_skipsExecution() {
        when(appFeatureService.isEnabled(FeatureKey.SCHEDULER)).thenReturn(false);
        scheduler = new JobSchedulerService(List.of(jobA), jobRunRepository, appFeatureService);

        scheduler.runDailyJobs();

        verifyNoInteractions(jobA);
        verifyNoInteractions(jobRunRepository);
    }
}
