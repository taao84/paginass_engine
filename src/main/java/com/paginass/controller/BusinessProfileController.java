package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileUpdateRequestDto;
import com.paginass.dto.ContributorBusinessCreateRequestDto;
import com.paginass.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile/businesses")
@RequiredArgsConstructor
public class BusinessProfileController {

    private final BusinessProfileService businessProfileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessProfileDetailDto createBusiness(
            @Valid @RequestBody ContributorBusinessCreateRequestDto request) {
        return businessProfileService.createOwnedBusiness(request);
    }

    @GetMapping
    public ResponseEntity<List<BusinessProfileDetailDto>> listMyBusinesses() {
        return ResponseEntity.ok(businessProfileService.getProfilesByOwner());
    }

    @GetMapping("/{id}/for-edit")
    public ResponseEntity<BusinessProfileDetailDto> getBusinessForEdit(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.getProfileById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessProfileDetailDto> updateBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessProfileUpdateRequestDto request) {
        return ResponseEntity.ok(businessProfileService.updateProfile(id, request));
    }

    @PostMapping(value = "/{id}/images/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BusinessProfileDetailDto> uploadBannerImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(businessProfileService.uploadBannerImage(id, file));
    }

    @DeleteMapping("/{id}/images/banner")
    public ResponseEntity<BusinessProfileDetailDto> deleteBannerImage(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.deleteBannerImage(id));
    }

    @PostMapping(value = "/{id}/images/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BusinessProfileDetailDto> uploadThumbnailImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(businessProfileService.uploadThumbnailImage(id, file));
    }

    @DeleteMapping("/{id}/images/thumbnail")
    public ResponseEntity<BusinessProfileDetailDto> deleteThumbnailImage(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.deleteThumbnailImage(id));
    }
}
