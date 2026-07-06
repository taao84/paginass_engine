package com.paginass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.CampaignCreateRequestDto;
import com.paginass.dto.CampaignDto;
import com.paginass.dto.CampaignStatusUpdateRequestDto;
import com.paginass.dto.NotifyWinnersRequestDto;
import com.paginass.dto.NotifyWinnersResponseDto;
import com.paginass.dto.PhotoContestEntryDto;
import com.paginass.dto.PhotoContestWinnerUpdateRequestDto;
import com.paginass.service.CampaignService;
import com.paginass.service.PhotoContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {

    private final CampaignService campaignService;
    private final PhotoContestService photoContestService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<CampaignDto>> listAll() {
        return ResponseEntity.ok(campaignService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignDto create(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "image", required = false) MultipartFile image) throws Exception {
        CampaignCreateRequestDto request = objectMapper.readValue(dataJson, CampaignCreateRequestDto.class);
        return campaignService.create(request, image);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CampaignDto> update(
            @PathVariable UUID id,
            @RequestParam("data") String dataJson,
            @RequestParam(value = "image", required = false) MultipartFile image) throws Exception {
        CampaignCreateRequestDto request = objectMapper.readValue(dataJson, CampaignCreateRequestDto.class);
        return ResponseEntity.ok(campaignService.update(id, request, image));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CampaignDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CampaignStatusUpdateRequestDto request) {
        return ResponseEntity.ok(campaignService.updateStatus(id, request));
    }

    @PostMapping("/{id}/businesses/{businessId}")
    public ResponseEntity<CampaignDto> linkBusiness(
            @PathVariable UUID id,
            @PathVariable UUID businessId) {
        return ResponseEntity.ok(campaignService.linkBusiness(id, businessId));
    }

    @DeleteMapping("/{id}/businesses/{businessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkBusiness(
            @PathVariable UUID id,
            @PathVariable UUID businessId) {
        campaignService.unlinkBusiness(id, businessId);
    }

    @PatchMapping("/{id}/entries/{entryId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreEntry(
            @PathVariable UUID id,
            @PathVariable UUID entryId) {
        photoContestService.restoreEntry(id, entryId);
    }

    @PatchMapping("/{id}/entries/{entryId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableEntry(
            @PathVariable UUID id,
            @PathVariable UUID entryId) {
        photoContestService.disableEntry(id, entryId);
    }

    @DeleteMapping("/{id}/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(
            @PathVariable UUID id,
            @PathVariable UUID entryId) {
        photoContestService.deleteEntry(id, entryId);
    }

    @PatchMapping("/{id}/entries/{entryId}/winner-rank")
    public ResponseEntity<PhotoContestEntryDto> setWinnerRank(
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @Valid @RequestBody PhotoContestWinnerUpdateRequestDto request) {
        return ResponseEntity.ok(photoContestService.setWinnerRank(id, entryId, request.winnerRank()));
    }

    @PostMapping("/{id}/notify-winners")
    public ResponseEntity<NotifyWinnersResponseDto> notifyWinners(
            @PathVariable UUID id,
            @Valid @RequestBody NotifyWinnersRequestDto request) {
        int notifiedCount = photoContestService.notifyWinners(id, request.subject(), request.message(), request.entryIds());
        return ResponseEntity.ok(new NotifyWinnersResponseDto(notifiedCount));
    }
}
