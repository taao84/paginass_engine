package com.paginass.controller;

import com.paginass.dto.BusinessMapMarkerDto;
import com.paginass.dto.BusinessMiniSiteDto;
import com.paginass.dto.DirectoryPageDto;
import com.paginass.dto.TagCloudItemDto;
import com.paginass.config.FeatureKey;
import com.paginass.dto.AppFeatureDto;
import com.paginass.service.AppFeatureService;
import com.paginass.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final BusinessProfileService businessProfileService;
    private final AppFeatureService appFeatureService;

    @GetMapping("/features")
    public List<AppFeatureDto> getFeatures() {
        return appFeatureService.getAll();
    }

    @GetMapping("/mini-site/{slug}")
    public ResponseEntity<BusinessMiniSiteDto> getMiniSite(@PathVariable String slug) {
        return ResponseEntity.ok(businessProfileService.getMiniSiteBySlug(slug));
    }

    @GetMapping("/directory")
    public ResponseEntity<DirectoryPageDto> getDirectory(
            @RequestParam(defaultValue = "0") int page) {
        if (!appFeatureService.isEnabled(FeatureKey.CLASSIC_DIRECTORY)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(businessProfileService.getDirectory(page));
    }

    @GetMapping("/directory/by-letter")
    public ResponseEntity<DirectoryPageDto> getDirectoryByLetter(
            @RequestParam String letter,
            @RequestParam(defaultValue = "0") int page) {
        if (!appFeatureService.isEnabled(FeatureKey.CLASSIC_DIRECTORY)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(businessProfileService.getDirectoryByLetter(letter, page));
    }

    @GetMapping("/directory/by-tag")
    public ResponseEntity<DirectoryPageDto> getDirectoryByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "0") int page) {
        if (!appFeatureService.isEnabled(FeatureKey.DIRECTORY_BY_CATEGORY)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(businessProfileService.getDirectoryByTag(tag, page));
    }

    @GetMapping("/directory/tags")
    public ResponseEntity<List<TagCloudItemDto>> getDirectoryTags() {
        if (!appFeatureService.isEnabled(FeatureKey.DIRECTORY_BY_CATEGORY)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(businessProfileService.getDirectoryTags());
    }

    @GetMapping("/business/{slug}")
    public ResponseEntity<BusinessMiniSiteDto> getPublicBusiness(@PathVariable String slug) {
        return ResponseEntity.ok(businessProfileService.getPublicBusinessBySlug(slug));
    }

    @GetMapping("/businesses/locations")
    public ResponseEntity<List<BusinessMapMarkerDto>> getBusinessLocations() {
        if (!appFeatureService.isEnabled(FeatureKey.BUSINESS_MAP)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(businessProfileService.getBusinessLocations());
    }

}
