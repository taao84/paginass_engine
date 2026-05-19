package com.paginass.controller;

import com.paginass.dto.AppNewsPageDto;
import com.paginass.service.AppNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/news")
@RequiredArgsConstructor
public class PublicNewsController {

    private final AppNewsService appNewsService;

    @GetMapping
    public ResponseEntity<AppNewsPageDto> getPublishedNews(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(appNewsService.getPublished(page));
    }
}
