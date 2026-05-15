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
    private static final String OVERLAY_COLOR_KEY = "banner_overlay_color";
    private static final String OVERLAY_OPACITY_KEY = "banner_overlay_opacity";
    private static final String OVERLAY_TEXT_KEY = "banner_overlay_text";
    private static final String OVERLAY_BORDER_KEY = "banner_overlay_border";
    private static final String OVERLAY_SPACING_KEY = "banner_overlay_spacing";
    private static final String OVERLAY_Y_KEY = "banner_overlay_y";
    private static final String DEFAULT_OVERLAY_COLOR = "#000000";
    private static final double DEFAULT_OVERLAY_OPACITY = 0.3;
    private static final String DEFAULT_OVERLAY_TEXT = "PAGINASS";
    private static final boolean DEFAULT_OVERLAY_BORDER = false;
    private static final int DEFAULT_OVERLAY_SPACING = 130;
    private static final int DEFAULT_OVERLAY_Y = 141;
    private static final long MAX_BANNER_BYTES = 4 * 1024 * 1024L;
    private static final int MAX_HISTORY = 12;

    private final AppSettingRepository appSettingRepository;
    private final BannerHistoryRepository bannerHistoryRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public BannerSettingDto getBannerSetting() {
        String imageUrl = appSettingRepository.findById(BANNER_KEY)
                .map(s -> storageService.toPublicUrl(s.getSettingValue()))
                .orElse(null);
        String overlayColor = appSettingRepository.findById(OVERLAY_COLOR_KEY)
                .map(AppSetting::getSettingValue)
                .orElse(DEFAULT_OVERLAY_COLOR);
        double overlayOpacity = appSettingRepository.findById(OVERLAY_OPACITY_KEY)
                .map(s -> Double.parseDouble(s.getSettingValue()))
                .orElse(DEFAULT_OVERLAY_OPACITY);
        String overlayText = appSettingRepository.findById(OVERLAY_TEXT_KEY)
                .map(AppSetting::getSettingValue)
                .orElse(DEFAULT_OVERLAY_TEXT);
        boolean overlayBorder = appSettingRepository.findById(OVERLAY_BORDER_KEY)
                .map(s -> Boolean.parseBoolean(s.getSettingValue()))
                .orElse(DEFAULT_OVERLAY_BORDER);
        int overlaySpacing = appSettingRepository.findById(OVERLAY_SPACING_KEY)
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(DEFAULT_OVERLAY_SPACING);
        int overlayY = appSettingRepository.findById(OVERLAY_Y_KEY)
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(DEFAULT_OVERLAY_Y);
        return new BannerSettingDto(imageUrl, overlayColor, overlayOpacity, overlayText, overlayBorder, overlaySpacing, overlayY);
    }

    @Transactional(readOnly = true)
    public List<BannerHistoryEntryDto> getBannerHistory() {
        return bannerHistoryRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(h -> new BannerHistoryEntryDto(h.getId(), storageService.toPublicUrl(h.getSubPath()), h.getUploadedAt()))
                .toList();
    }

    @Transactional
    public void updateOverlay(String color, double opacity, String text, boolean border, int spacing, int y) {
        saveSetting(OVERLAY_COLOR_KEY, color);
        saveSetting(OVERLAY_OPACITY_KEY, String.valueOf(opacity));
        saveSetting(OVERLAY_TEXT_KEY, text);
        saveSetting(OVERLAY_BORDER_KEY, String.valueOf(border));
        saveSetting(OVERLAY_SPACING_KEY, String.valueOf(spacing));
        saveSetting(OVERLAY_Y_KEY, String.valueOf(y));
    }

    public void uploadBanner(MultipartFile file) {
        if (file.getSize() > MAX_BANNER_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must not exceed 4 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an image");
        }

        String subPath = "banner/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());
        try {
            storageService.saveFile(subPath, file.getInputStream(), file.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save banner image");
        }

        bannerHistoryRepository.save(new BannerHistory(subPath));
        pruneHistory();
        saveSetting(BANNER_KEY, subPath);
    }

    @Transactional
    public void applyHistoryEntry(UUID id) {
        BannerHistory entry = bannerHistoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History entry not found"));
        saveSetting(BANNER_KEY, entry.getSubPath());
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

    private void saveSetting(String key, String value) {
        AppSetting setting = appSettingRepository.findById(key).orElseGet(AppSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setUpdatedAt(OffsetDateTime.now());
        appSettingRepository.save(setting);
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
