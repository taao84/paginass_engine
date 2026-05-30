package com.paginass.controller;

import com.paginass.dto.BannerHistoryEntryDto;
import com.paginass.dto.BannerOverlayRequestDto;
import com.paginass.dto.BannerSettingDto;
import com.paginass.dto.NotificationEmailDto;
import com.paginass.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/admin/settings/notification-email")
    public ResponseEntity<NotificationEmailDto> getNotificationEmail() {
        String email = settingsService.getAdminNotificationEmail().orElse("");
        return ResponseEntity.ok(new NotificationEmailDto(email));
    }

    @PutMapping("/admin/settings/notification-email")
    public ResponseEntity<Void> updateNotificationEmail(@RequestBody NotificationEmailDto request) {
        settingsService.updateAdminNotificationEmail(request.email());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/settings/banner")
    public ResponseEntity<BannerSettingDto> getBanner() {
        return ResponseEntity.ok(settingsService.getBannerSetting());
    }

    @PutMapping("/admin/settings/banner/overlay")
    public ResponseEntity<Void> updateOverlay(@Valid @RequestBody BannerOverlayRequestDto request) {
        settingsService.updateOverlay(request.overlayColor(), request.overlayOpacity(), request.overlayText(), request.overlayBorder(), request.overlaySpacing(), request.overlayY());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/admin/settings/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadBanner(@RequestParam("file") MultipartFile file) {
        settingsService.uploadBanner(file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/settings/banner")
    public ResponseEntity<Void> deleteBanner() {
        settingsService.deleteBanner();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/settings/banner/history")
    public ResponseEntity<List<BannerHistoryEntryDto>> getBannerHistory() {
        return ResponseEntity.ok(settingsService.getBannerHistory());
    }

    @PutMapping("/admin/settings/banner/history/{id}")
    public ResponseEntity<Void> applyHistoryEntry(@PathVariable UUID id) {
        settingsService.applyHistoryEntry(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/settings/banner/history/{id}")
    public ResponseEntity<Void> deleteHistoryEntry(@PathVariable UUID id) {
        settingsService.deleteHistoryEntry(id);
        return ResponseEntity.noContent().build();
    }
}
