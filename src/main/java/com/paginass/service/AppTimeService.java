package com.paginass.service;

import com.paginass.entity.AppSetting;
import com.paginass.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppTimeService {

    public static final String APP_TIMEZONE_KEY = "app_timezone";
    public static final String DEFAULT_TIMEZONE = "America/Bogota";

    private static final Locale DISPLAY_LOCALE = new Locale("es", "CO");
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter LOCAL_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AppSettingRepository appSettingRepository;

    @Transactional(readOnly = true)
    public String getTimezoneId() {
        return appSettingRepository.findById(APP_TIMEZONE_KEY)
                .map(AppSetting::getSettingValue)
                .filter(v -> !v.isBlank())
                .orElse(DEFAULT_TIMEZONE);
    }

    @Transactional(readOnly = true)
    public ZoneId getZoneId() {
        return ZoneId.of(getTimezoneId());
    }

    @Transactional
    public String updateTimezone(String zoneId) {
        try {
            ZoneId.of(zoneId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona horaria inválida: " + zoneId);
        }
        AppSetting setting = appSettingRepository.findById(APP_TIMEZONE_KEY).orElseGet(AppSetting::new);
        setting.setSettingKey(APP_TIMEZONE_KEY);
        setting.setSettingValue(zoneId);
        setting.setGlobal(true);
        setting.setUpdatedAt(OffsetDateTime.now());
        appSettingRepository.save(setting);
        return zoneId;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(getZoneId());
    }

    public OffsetDateTime toInstant(LocalDateTime naiveLocal) {
        return naiveLocal.atZone(getZoneId()).toOffsetDateTime();
    }

    public String formatDisplay(OffsetDateTime value) {
        return value.atZoneSameInstant(getZoneId()).format(DISPLAY_FORMATTER);
    }

    public String formatLocalInput(OffsetDateTime value) {
        return value.atZoneSameInstant(getZoneId()).format(LOCAL_INPUT_FORMATTER);
    }
}
