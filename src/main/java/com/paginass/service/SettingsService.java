package com.paginass.service;

import com.paginass.dto.BannerHistoryEntryDto;
import com.paginass.dto.BannerSettingDto;
import com.paginass.entity.AppSetting;
import com.paginass.entity.BannerHistory;
import com.paginass.repository.AppSettingRepository;
import com.paginass.repository.BannerHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String BANNER_KEY = "banner";
    private static final long MAX_BANNER_BYTES = 3 * 1024 * 1024L;
    private static final int MAX_HISTORY = 12;

    private final AppSettingRepository appSettingRepository;
    private final BannerHistoryRepository bannerHistoryRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public BannerSettingDto getBannerSetting() {
        return appSettingRepository.findById(BANNER_KEY)
                .map(s -> new BannerSettingDto(storageService.toPublicUrl(s.getSettingValue())))
                .orElse(new BannerSettingDto(null));
    }

    @Transactional(readOnly = true)
    public List<BannerHistoryEntryDto> getBannerHistory() {
        return bannerHistoryRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(h -> new BannerHistoryEntryDto(h.getId(), storageService.toPublicUrl(h.getSubPath()), h.getUploadedAt()))
                .toList();
    }

    public void uploadBanner(MultipartFile file) {
        if (file.getSize() > MAX_BANNER_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must not exceed 3 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an image");
        }

        String subPath = "banner/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());
        try {
            storageService.saveFile(subPath, file.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save banner image");
        }

        bannerHistoryRepository.save(new BannerHistory(subPath));
        pruneHistory();

        AppSetting setting = appSettingRepository.findById(BANNER_KEY).orElseGet(AppSetting::new);
        setting.setSettingKey(BANNER_KEY);
        setting.setSettingValue(subPath);
        setting.setUpdatedAt(OffsetDateTime.now());
        appSettingRepository.save(setting);
    }

    @Transactional
    public void applyHistoryEntry(UUID id) {
        BannerHistory entry = bannerHistoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History entry not found"));

        AppSetting setting = appSettingRepository.findById(BANNER_KEY).orElseGet(AppSetting::new);
        setting.setSettingKey(BANNER_KEY);
        setting.setSettingValue(entry.getSubPath());
        setting.setUpdatedAt(OffsetDateTime.now());
        appSettingRepository.save(setting);
    }

    public void deleteHistoryEntry(UUID id) {
        BannerHistory entry = bannerHistoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History entry not found"));

        bannerHistoryRepository.delete(entry);

        appSettingRepository.findById(BANNER_KEY).ifPresent(setting -> {
            if (entry.getSubPath().equals(setting.getSettingValue())) {
                appSettingRepository.delete(setting);
            }
        });

        try {
            storageService.deleteFile(entry.getSubPath());
        } catch (IOException ignored) {}
    }

    public void deleteBanner() {
        appSettingRepository.findById(BANNER_KEY).ifPresent(appSettingRepository::delete);
    }

    private void pruneHistory() {
        List<BannerHistory> all = bannerHistoryRepository.findAllByOrderByUploadedAtDesc();
        if (all.size() > MAX_HISTORY) {
            List<BannerHistory> toDelete = all.subList(MAX_HISTORY, all.size());
            for (BannerHistory entry : toDelete) {
                bannerHistoryRepository.delete(entry);
                try {
                    storageService.deleteFile(entry.getSubPath());
                } catch (IOException ignored) {}
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
