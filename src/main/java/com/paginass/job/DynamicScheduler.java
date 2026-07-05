package com.paginass.job;

import com.paginass.service.AppTimeService;
import com.paginass.service.JobSchedulerService;
import com.paginass.service.SchedulerTimeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicScheduler {

    private final TaskScheduler taskScheduler;
    private final SchedulerTimeService schedulerTimeService;
    private final JobSchedulerService jobSchedulerService;
    private final AppTimeService appTimeService;

    private ScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void init() {
        try {
            reschedule(schedulerTimeService.getSchedulerTime());
        } catch (Exception e) {
            log.warn("Could not load scheduler time from DB — using default ({}): {}",
                    SchedulerTimeService.DEFAULT_SCHEDULER_TIME, e.getMessage());
            reschedule(SchedulerTimeService.DEFAULT_SCHEDULER_TIME);
        }
    }

    public synchronized void reschedule(String time) {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        String cron = String.format("0 %d %d * * *", minute, hour);
        ZoneId zone = appTimeService.getZoneId();
        scheduledFuture = taskScheduler.schedule(
                jobSchedulerService::runDailyJobs,
                new CronTrigger(cron, zone)
        );
        log.info("Daily job scheduler configured for {}:{} {} (cron={})",
                String.format("%02d", hour), String.format("%02d", minute), zone, cron);
    }
}
