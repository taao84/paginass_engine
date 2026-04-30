package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileEditDto;
import com.paginass.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminBusinessProfileController {

    private final BusinessProfileService businessProfileService;

    @GetMapping("/api/admin/business-profiles/{id}")
    public ResponseEntity<BusinessProfileDetailDto> getBusinessDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.getProfileById(id));
    }

    @GetMapping("/api/admin/business-profiles/{id}/edits")
    public ResponseEntity<List<BusinessProfileEditDto>> getEditHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(businessProfileService.getEditHistory(id));
    }

    @PostMapping("/api/admin/business-profiles/{id}/edits/{editId}/restore")
    public ResponseEntity<BusinessProfileDetailDto> restore(
            @PathVariable UUID id,
            @PathVariable UUID editId) {
        return ResponseEntity.ok(businessProfileService.restoreFromEdit(id, editId));
    }
}
