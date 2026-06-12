package com.paginass.controller;

import com.paginass.dto.AdvertisementAdminPageDto;
import com.paginass.service.AdvertisementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
public class AdminAdvertisementController {

    private final AdvertisementService advertisementService;

    @GetMapping
    public ResponseEntity<AdvertisementAdminPageDto> listAds(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(advertisementService.getAdminAds(page));
    }

    @PatchMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableAd(@PathVariable UUID id) {
        advertisementService.disableAdvertisement(id);
    }

    @PatchMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableAd(@PathVariable UUID id) {
        advertisementService.enableAdvertisement(id);
    }
}
