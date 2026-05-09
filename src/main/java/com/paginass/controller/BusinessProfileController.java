package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileUpdateRequestDto;
import com.paginass.dto.ContributorBusinessCreateRequestDto;
import com.paginass.dto.ImageOrderRequestDto;
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

    @GetMapping("/{id}")
    public ResponseEntity<BusinessProfileDetailDto> getBusiness(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.getProfileByOwner(id));
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

    @PatchMapping("/{id}/mini-site")
    public ResponseEntity<BusinessProfileDetailDto> toggleMiniSite(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.toggleMiniSite(id));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BusinessProfileDetailDto> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(businessProfileService.uploadImage(id, file));
    }

    @PutMapping("/{id}/images/order")
    public ResponseEntity<BusinessProfileDetailDto> reorderImages(
            @PathVariable UUID id,
            @RequestBody ImageOrderRequestDto request) {
        return ResponseEntity.ok(businessProfileService.reorderImages(id, request.imageUrls()));
    }

    @DeleteMapping("/{id}/images/{index}")
    public ResponseEntity<BusinessProfileDetailDto> deleteImage(
            @PathVariable UUID id,
            @PathVariable int index) {
        return ResponseEntity.ok(businessProfileService.deleteImage(id, index));
    }
}
