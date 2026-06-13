package com.paginass.controller;

import com.paginass.dto.AdvertisementResponseDto;
import com.paginass.dto.AdvertisementSummaryDto;
import com.paginass.dto.PublicAdsPageDto;
import com.paginass.entity.AdCategory;
import com.paginass.service.AdvertisementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/ads")
@RequiredArgsConstructor
public class PublicAdvertisementController {

    private final AdvertisementService advertisementService;

    @GetMapping
    public PublicAdsPageDto listAds(
            @RequestParam(required = false) AdCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page) {
        return advertisementService.getActiveAdvertisementsPage(category, search, page);
    }

    @GetMapping("/{id}")
    public AdvertisementResponseDto getAd(@PathVariable UUID id) {
        return advertisementService.getAdvertisementById(id);
    }

    @GetMapping("/business/{businessId}/events")
    public List<AdvertisementSummaryDto> listBusinessEvents(@PathVariable UUID businessId) {
        return advertisementService.getBusinessEvents(businessId);
    }
}
