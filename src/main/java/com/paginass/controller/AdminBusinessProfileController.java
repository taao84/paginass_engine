package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileEditDto;
import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import com.paginass.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminBusinessProfileController {

    private final BusinessProfileService businessProfileService;
    private final UserRepository userRepository;

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
            @PathVariable UUID editId,
            Authentication authentication) {
        User admin = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.restoreFromEdit(id, editId, admin));
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
