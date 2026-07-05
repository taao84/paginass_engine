package com.paginass.service;

import com.paginass.entity.AppSetting;
import com.paginass.repository.AppSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTimeServiceTest {

    @Mock private AppSettingRepository appSettingRepository;

    @InjectMocks
    private AppTimeService appTimeService;

    @Test
    void getTimezoneId_noSettingStored_returnsDefault() {
        when(appSettingRepository.findById("app_timezone")).thenReturn(Optional.empty());

        assertThat(appTimeService.getTimezoneId()).isEqualTo("America/Bogota");
    }

    @Test
    void getTimezoneId_settingStored_returnsStoredValue() {
        when(appSettingRepository.findById("app_timezone"))
                .thenReturn(Optional.of(settingWith("Europe/Madrid")));

        assertThat(appTimeService.getTimezoneId()).isEqualTo("Europe/Madrid");
    }

    @Test
    void updateTimezone_validZone_savesGlobalSetting() {
        when(appSettingRepository.findById("app_timezone")).thenReturn(Optional.empty());
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appTimeService.updateTimezone("America/Bogota");

        ArgumentCaptor<AppSetting> captor = ArgumentCaptor.forClass(AppSetting.class);
        org.mockito.Mockito.verify(appSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingKey()).isEqualTo("app_timezone");
        assertThat(captor.getValue().getSettingValue()).isEqualTo("America/Bogota");
        assertThat(captor.getValue().isGlobal()).isTrue();
    }

    @Test
    void updateTimezone_invalidZone_throws400() {
        assertThatThrownBy(() -> appTimeService.updateTimezone("Not/AZone"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void toInstant_combinesNaiveLocalDatetimeWithConfiguredZone() {
        when(appSettingRepository.findById("app_timezone"))
                .thenReturn(Optional.of(settingWith("America/Bogota")));

        OffsetDateTime result = appTimeService.toInstant(LocalDateTime.of(2026, 7, 5, 16, 0));

        assertThat(result).isEqualTo(
                LocalDateTime.of(2026, 7, 5, 16, 0).atZone(ZoneId.of("America/Bogota")).toOffsetDateTime());
    }

    @Test
    void now_usesConfiguredZoneOffset() {
        when(appSettingRepository.findById("app_timezone"))
                .thenReturn(Optional.of(settingWith("America/Bogota")));

        OffsetDateTime result = appTimeService.now();

        assertThat(result.getOffset()).isEqualTo(ZoneId.of("America/Bogota").getRules().getOffset(result.toLocalDateTime()));
    }

    @Test
    void formatLocalInput_returnsNaiveDatetimeStringInConfiguredZone() {
        when(appSettingRepository.findById("app_timezone"))
                .thenReturn(Optional.of(settingWith("America/Bogota")));

        OffsetDateTime value = LocalDateTime.of(2026, 7, 5, 16, 0).atZone(ZoneId.of("America/Bogota")).toOffsetDateTime();

        assertThat(appTimeService.formatLocalInput(value)).isEqualTo("2026-07-05T16:00");
    }

    private AppSetting settingWith(String value) {
        AppSetting s = new AppSetting();
        s.setSettingKey("app_timezone");
        s.setSettingValue(value);
        return s;
    }
}
