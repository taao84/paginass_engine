package com.paginass.service;

import com.paginass.entity.AppSetting;
import com.paginass.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SchedulerTimeService {

    public static final String SCHEDULER_TIME_KEY = "scheduler_time";
    public static final String DEFAULT_SCHEDULER_TIME = "01:00";

    private final AppSettingRepository appSettingRepository;

    @Transactional(readOnly = true)
    public String getSchedulerTime() {
        return appSettingRepository.findById(SCHEDULER_TIME_KEY)
                .map(AppSetting::getSettingValue)
                .orElse(DEFAULT_SCHEDULER_TIME);
    }

    @Transactional
    public String updateSchedulerTime(String time) {
        if (!time.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de hora inválido. Use HH:MM (00:00–23:59)");
        }
        AppSetting setting = appSettingRepository.findById(SCHEDULER_TIME_KEY).orElseGet(AppSetting::new);
        setting.setSettingKey(SCHEDULER_TIME_KEY);
        setting.setSettingValue(time);
        setting.setUpdatedAt(OffsetDateTime.now());
        appSettingRepository.save(setting);
        return time;
    }
}
