package com.paginass.controller;

import com.paginass.dto.AdImageOrderRequestDto;
import com.paginass.dto.AdvertisementRequestDto;
import com.paginass.dto.AdvertisementResponseDto;
import com.paginass.dto.AdvertisementSummaryDto;
import com.paginass.dto.AdvertisementUpdateRequestDto;
import com.paginass.service.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdvertisementResponseDto createAd(@Valid @RequestBody AdvertisementRequestDto request) {
        return advertisementService.createAdvertisement(request);
    }

    @PostMapping("/{id}/images")
    public AdvertisementResponseDto uploadImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        return advertisementService.addImages(id, files);
    }

    @GetMapping("/my")
    public List<AdvertisementSummaryDto> myAds() {
        return advertisementService.getMyAdvertisements();
    }

    @PutMapping("/{id}")
    public AdvertisementResponseDto updateAd(
            @PathVariable UUID id,
            @Valid @RequestBody AdvertisementUpdateRequestDto request) {
        return advertisementService.updateAdvertisement(id, request);
    }

    @DeleteMapping("/{id}/images/{index}")
    public AdvertisementResponseDto deleteImage(
            @PathVariable UUID id,
            @PathVariable int index) {
        advertisementService.deleteImage(id, index);
        return advertisementService.getAdvertisementById(id);
    }

    @PutMapping("/{id}/images/order")
    public AdvertisementResponseDto reorderImages(
            @PathVariable UUID id,
            @RequestBody AdImageOrderRequestDto request) {
        return advertisementService.reorderImages(id, request.indices());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAd(@PathVariable UUID id) {
        advertisementService.deleteAdvertisement(id);
    }
}
