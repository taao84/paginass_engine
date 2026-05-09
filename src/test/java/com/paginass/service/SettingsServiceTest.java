package com.paginass.service;

import com.paginass.dto.BannerHistoryEntryDto;
import com.paginass.dto.BannerSettingDto;
import com.paginass.entity.AppSetting;
import com.paginass.entity.BannerHistory;
import com.paginass.repository.AppSettingRepository;
import com.paginass.repository.BannerHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private AppSettingRepository appSettingRepository;
    @Mock private BannerHistoryRepository bannerHistoryRepository;
    @Mock private StorageService storageService;

    @InjectMocks private SettingsService settingsService;

    // --- getBannerSetting ---

    @Test
    void getBannerSetting_noBannerSet_returnsNullValue() {
        when(appSettingRepository.findById("banner")).thenReturn(Optional.empty());

        BannerSettingDto result = settingsService.getBannerSetting();

        assertThat(result.value()).isNull();
    }

    @Test
    void getBannerSetting_bannerExists_returnsPublicUrl() {
        AppSetting setting = settingWith("banner/abc.jpg");
        when(appSettingRepository.findById("banner")).thenReturn(Optional.of(setting));
        when(storageService.toPublicUrl("banner/abc.jpg")).thenReturn("http://localhost/uploads/banner/abc.jpg");

        BannerSettingDto result = settingsService.getBannerSetting();

        assertThat(result.value()).isEqualTo("http://localhost/uploads/banner/abc.jpg");
    }

    // --- uploadBanner ---

    @Test
    void uploadBanner_fileTooLarge_throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[4 * 1024 * 1024]);

        assertThatThrownBy(() -> settingsService.uploadBanner(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void uploadBanner_nonImageContentType_throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);

        assertThatThrownBy(() -> settingsService.uploadBanner(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void uploadBanner_validFile_savesHistoryEntryAndSetting() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[100]);
        when(storageService.saveFile(anyString(), any())).thenReturn("banner/new.jpg");
        when(bannerHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bannerHistoryRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of());
        when(appSettingRepository.findById("banner")).thenReturn(Optional.empty());
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        settingsService.uploadBanner(file);

        verify(bannerHistoryRepository).save(any(BannerHistory.class));
        ArgumentCaptor<AppSetting> captor = ArgumentCaptor.forClass(AppSetting.class);
        verify(appSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingKey()).isEqualTo("banner");
        assertThat(captor.getValue().getSettingValue()).startsWith("banner/");
    }

    @Test
    void uploadBanner_thirteenthUpload_prunesOldestEntry() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[100]);
        when(storageService.saveFile(anyString(), any())).thenReturn("banner/new.jpg");
        when(bannerHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appSettingRepository.findById("banner")).thenReturn(Optional.empty());
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<BannerHistory> history = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            BannerHistory h = new BannerHistory("banner/old-" + i + ".jpg");
            history.add(h);
        }
        when(bannerHistoryRepository.findAllByOrderByUploadedAtDesc()).thenReturn(history);

        settingsService.uploadBanner(file);

        verify(bannerHistoryRepository, times(1)).delete(any(BannerHistory.class));
        verify(storageService, times(1)).deleteFile(anyString());
    }

    // --- applyHistoryEntry ---

    @Test
    void applyHistoryEntry_existingEntry_updatesActiveSetting() {
        UUID id = UUID.randomUUID();
        BannerHistory entry = new BannerHistory("banner/old.jpg");
        when(bannerHistoryRepository.findById(id)).thenReturn(Optional.of(entry));
        when(appSettingRepository.findById("banner")).thenReturn(Optional.empty());
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        settingsService.applyHistoryEntry(id);

        ArgumentCaptor<AppSetting> captor = ArgumentCaptor.forClass(AppSetting.class);
        verify(appSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingValue()).isEqualTo("banner/old.jpg");
    }

    @Test
    void applyHistoryEntry_unknownId_throws404() {
        UUID id = UUID.randomUUID();
        when(bannerHistoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.applyHistoryEntry(id))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- deleteHistoryEntry ---

    @Test
    void deleteHistoryEntry_notActiveEntry_deletesFileAndRow() throws IOException {
        UUID id = UUID.randomUUID();
        BannerHistory entry = new BannerHistory("banner/old.jpg");
        AppSetting active = settingWith("banner/current.jpg");
        when(bannerHistoryRepository.findById(id)).thenReturn(Optional.of(entry));
        when(appSettingRepository.findById("banner")).thenReturn(Optional.of(active));

        settingsService.deleteHistoryEntry(id);

        verify(bannerHistoryRepository).delete(entry);
        verify(storageService).deleteFile("banner/old.jpg");
        verify(appSettingRepository, never()).delete(any());
    }

    @Test
    void deleteHistoryEntry_activeEntry_deletesFileRowAndClearsSetting() throws IOException {
        UUID id = UUID.randomUUID();
        BannerHistory entry = new BannerHistory("banner/current.jpg");
        AppSetting active = settingWith("banner/current.jpg");
        when(bannerHistoryRepository.findById(id)).thenReturn(Optional.of(entry));
        when(appSettingRepository.findById("banner")).thenReturn(Optional.of(active));

        settingsService.deleteHistoryEntry(id);

        verify(bannerHistoryRepository).delete(entry);
        verify(appSettingRepository).delete(active);
        verify(storageService).deleteFile("banner/current.jpg");
    }

    // --- deleteBanner ---

    @Test
    void deleteBanner_settingExists_clearsSettingOnly() {
        AppSetting setting = settingWith("banner/xyz.jpg");
        when(appSettingRepository.findById("banner")).thenReturn(Optional.of(setting));

        settingsService.deleteBanner();

        verify(appSettingRepository).delete(setting);
        verify(bannerHistoryRepository, never()).delete(any());
    }

    @Test
    void deleteBanner_noSettingSet_doesNothing() {
        when(appSettingRepository.findById("banner")).thenReturn(Optional.empty());

        settingsService.deleteBanner();

        verify(appSettingRepository, never()).delete(any());
    }

    // --- getBannerHistory ---

    @Test
    void getBannerHistory_returnsEntriesMappedToPublicUrls() {
        BannerHistory h = new BannerHistory("banner/abc.jpg");
        when(bannerHistoryRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of(h));
        when(storageService.toPublicUrl("banner/abc.jpg")).thenReturn("http://localhost/uploads/banner/abc.jpg");

        List<BannerHistoryEntryDto> result = settingsService.getBannerHistory();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).url()).isEqualTo("http://localhost/uploads/banner/abc.jpg");
    }

    // --- helpers ---

    private AppSetting settingWith(String subPath) {
        AppSetting s = new AppSetting();
        s.setSettingKey("banner");
        s.setSettingValue(subPath);
        s.setUpdatedAt(OffsetDateTime.now());
        return s;
    }
}
