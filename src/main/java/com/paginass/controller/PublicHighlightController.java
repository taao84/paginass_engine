package com.paginass.controller;

import com.paginass.dto.HighlightCardDto;
import com.paginass.service.HighlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/highlights")
@RequiredArgsConstructor
public class PublicHighlightController {

    private final HighlightService highlightService;

    @GetMapping
    public ResponseEntity<List<HighlightCardDto>> getActiveHighlights() {
        return ResponseEntity.ok(highlightService.getActiveHighlights());
    }
}
