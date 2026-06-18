package com.paginass.controller;

import com.paginass.dto.BusinessConfigurationDto;
import com.paginass.service.AdminBusinessConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/businesses")
@RequiredArgsConstructor
public class AdminBusinessConfigurationController {

    private final AdminBusinessConfigurationService adminBusinessConfigurationService;

    @GetMapping("/{id}/configuration")
    public ResponseEntity<BusinessConfigurationDto> getConfiguration(@PathVariable UUID id) {
        return ResponseEntity.ok(adminBusinessConfigurationService.getConfiguration(id));
    }

    @PutMapping("/{id}/configuration")
    public ResponseEntity<BusinessConfigurationDto> updateConfiguration(
            @PathVariable UUID id,
            @RequestBody BusinessConfigurationDto request) {
        return ResponseEntity.ok(adminBusinessConfigurationService.updateConfiguration(id, request));
    }
}
