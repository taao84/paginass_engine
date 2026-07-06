package com.paginass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.CampaignDto;
import com.paginass.dto.PhotoContestEntryCreateRequestDto;
import com.paginass.dto.PhotoContestEntryDto;
import com.paginass.dto.PhotoContestLikeDto;
import com.paginass.service.CampaignService;
import com.paginass.service.PhotoContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class PhotoContestController {

    private final PhotoContestService photoContestService;
    private final CampaignService campaignService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{slug}")
    public ResponseEntity<CampaignDto> getCampaign(@PathVariable String slug) {
        return ResponseEntity.ok(campaignService.getBySlug(slug));
    }

    @GetMapping("/{slug}/entries")
    public ResponseEntity<List<PhotoContestEntryDto>> getGallery(@PathVariable String slug) {
        return ResponseEntity.ok(photoContestService.getGallery(slug));
    }

    @GetMapping("/{slug}/results")
    public ResponseEntity<List<PhotoContestEntryDto>> getResults(@PathVariable String slug) {
        return ResponseEntity.ok(photoContestService.getResults(slug));
    }

    @GetMapping("/entries/{entryId}")
    public ResponseEntity<PhotoContestEntryDto> getEntry(@PathVariable UUID entryId) {
        return ResponseEntity.ok(photoContestService.getEntry(entryId));
    }

    @PostMapping(value = "/{slug}/entries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoContestEntryDto submitEntry(
            @PathVariable String slug,
            @RequestParam("data") String dataJson,
            @RequestParam("photo") MultipartFile photo) throws Exception {
        PhotoContestEntryCreateRequestDto request = objectMapper.readValue(dataJson, PhotoContestEntryCreateRequestDto.class);
        return photoContestService.submitEntry(slug, request, photo);
    }

    @PostMapping("/entries/{entryId}/like")
    public ResponseEntity<PhotoContestLikeDto> toggleLike(@PathVariable UUID entryId) {
        return ResponseEntity.ok(photoContestService.toggleLike(entryId));
    }
}
