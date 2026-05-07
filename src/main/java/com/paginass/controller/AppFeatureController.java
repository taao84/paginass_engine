package com.paginass.controller;

import com.paginass.dto.AppFeatureDto;
import com.paginass.dto.AppFeatureUpdateRequestDto;
import com.paginass.service.AppFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/features")
@RequiredArgsConstructor
public class AppFeatureController {

    private final AppFeatureService appFeatureService;

    @GetMapping
    public List<AppFeatureDto> getAll() {
        return appFeatureService.getAll();
    }

    @PatchMapping("/{key}")
    public AppFeatureDto setEnabled(
            @PathVariable String key,
            @RequestBody AppFeatureUpdateRequestDto request) {
        return appFeatureService.setEnabled(key, request.enabled());
    }
}
