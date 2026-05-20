package com.paginass.controller;

import com.paginass.dto.JobRunDto;
import com.paginass.dto.SchedulerTimeDto;
import com.paginass.job.DynamicScheduler;
import com.paginass.repository.JobRunRepository;
import com.paginass.service.JobSchedulerService;
import com.paginass.service.SchedulerTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
public class AdminSchedulerController {

    private final JobSchedulerService jobSchedulerService;
    private final JobRunRepository jobRunRepository;
    private final SchedulerTimeService schedulerTimeService;
    private final DynamicScheduler dynamicScheduler;

    @PostMapping("/trigger")
    public ResponseEntity<Void> trigger() {
        jobSchedulerService.triggerManually();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/runs")
    public List<JobRunDto> getRuns() {
        return jobRunRepository.findTop50ByOrderByStartedAtDesc().stream()
                .map(r -> new JobRunDto(
                        r.getId(),
                        r.getJobName(),
                        r.getTriggeredBy().name(),
                        r.getStartedAt(),
                        r.getFinishedAt(),
                        r.getStatus() != null ? r.getStatus().name() : null,
                        r.getMessage()
                ))
                .toList();
    }

    @GetMapping("/time")
    public SchedulerTimeDto getTime() {
        return new SchedulerTimeDto(schedulerTimeService.getSchedulerTime());
    }

    @PatchMapping("/time")
    public SchedulerTimeDto updateTime(@RequestBody SchedulerTimeDto request) {
        String saved = schedulerTimeService.updateSchedulerTime(request.time());
        dynamicScheduler.reschedule(saved);
        return new SchedulerTimeDto(saved);
    }
}
