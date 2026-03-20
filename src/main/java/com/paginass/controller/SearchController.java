package com.paginass.controller;

import com.paginass.dto.SearchResponseDto;
import com.paginass.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final BusinessProfileService businessProfileService;

    @GetMapping
    public ResponseEntity<SearchResponseDto> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page) {

        if (q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(businessProfileService.search(q.trim(), page));
    }
}
