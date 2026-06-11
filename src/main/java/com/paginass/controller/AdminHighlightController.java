package com.paginass.controller;

import com.paginass.dto.CreateHighlightRequestDto;
import com.paginass.dto.HighlightableAdDto;
import com.paginass.dto.HighlightableBusinessDto;
import com.paginass.dto.HighlightableCampaignDto;
import com.paginass.dto.HighlightSummaryDto;
import com.paginass.service.HighlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/highlights")
@RequiredArgsConstructor
public class AdminHighlightController {

    private final HighlightService highlightService;

    @PostMapping
    public ResponseEntity<HighlightSummaryDto> createHighlight(
            @Valid @RequestBody CreateHighlightRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(highlightService.createHighlight(request));
    }

    @GetMapping
    public ResponseEntity<List<HighlightSummaryDto>> listHighlights() {
        return ResponseEntity.ok(highlightService.listAdminHighlights());
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disableHighlight(@PathVariable UUID id) {
        highlightService.disableHighlight(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/businesses")
    public ResponseEntity<List<HighlightableBusinessDto>> searchBusinesses(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(highlightService.searchBusinesses(q));
    }

    @GetMapping("/search/ads")
    public ResponseEntity<List<HighlightableAdDto>> searchAds(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(highlightService.searchAds(q));
    }

    @GetMapping("/search/campaigns")
    public ResponseEntity<List<HighlightableCampaignDto>> searchCampaigns(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(highlightService.searchCampaigns(q));
    }
}
