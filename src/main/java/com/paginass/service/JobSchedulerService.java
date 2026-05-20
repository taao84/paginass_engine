package com.paginass.service;

import com.paginass.config.FeatureKey;
import com.paginass.entity.JobRun;
import com.paginass.entity.JobRunStatus;
import com.paginass.entity.JobRunTrigger;
import com.paginass.job.JobService;
import com.paginass.repository.JobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobSchedulerService {

    private final List<JobService> jobs;
    private final JobRunRepository jobRunRepository;
    private final AppFeatureService appFeatureService;

    @SchedulerLock(name = "dailyJobRun", lockAtMostFor = "PT5M")
    public void runDailyJobs() {
        if (!appFeatureService.isEnabled(FeatureKey.SCHEDULER)) {
            log.warn("Automatic scheduler is disabled — skipping daily job run");
            return;
        }
        executeJobs(JobRunTrigger.SCHEDULER);
    }

    public void triggerManually() {
        executeJobs(JobRunTrigger.ADMIN);
    }

    private void executeJobs(JobRunTrigger trigger) {
        log.info("Job run started — trigger={}, jobs={}", trigger, jobs.size());
        for (JobService job : jobs) {
            JobRun run = new JobRun();
            run.setJobName(job.jobName());
            run.setTriggeredBy(trigger);
            run.setStartedAt(Instant.now());
            try {
                job.execute();
                run.setStatus(JobRunStatus.SUCCESS);
                log.info("Job completed: {}", job.jobName());
            } catch (Exception e) {
                run.setStatus(JobRunStatus.FAILED);
                run.setMessage(e.getMessage());
                log.error("Job failed: {} — {}", job.jobName(), e.getMessage(), e);
            } finally {
                run.setFinishedAt(Instant.now());
                jobRunRepository.save(run);
            }
        }
        log.info("Job run finished — trigger={}", trigger);
    }
}
