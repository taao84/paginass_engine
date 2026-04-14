package com.paginass.controller;

import com.paginass.dto.BusinessMiniSiteDto;
import com.paginass.dto.DirectoryPageDto;
import com.paginass.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final BusinessProfileService businessProfileService;

    @GetMapping("/mini-site/{slug}")
    public ResponseEntity<BusinessMiniSiteDto> getMiniSite(@PathVariable String slug) {
        return ResponseEntity.ok(businessProfileService.getMiniSiteBySlug(slug));
    }

    @GetMapping("/directory")
    public ResponseEntity<DirectoryPageDto> getDirectory(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(businessProfileService.getDirectory(page));
    }

    @GetMapping("/business/{slug}")
    public ResponseEntity<BusinessMiniSiteDto> getPublicBusiness(@PathVariable String slug) {
        return ResponseEntity.ok(businessProfileService.getPublicBusinessBySlug(slug));
    }
}
