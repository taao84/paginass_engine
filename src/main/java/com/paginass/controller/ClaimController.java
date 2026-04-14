package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.ClaimRequestDto;
import com.paginass.dto.ClaimResponseDto;
import com.paginass.dto.ResolveClaimRequestDto;
import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import com.paginass.service.BusinessProfileService;
import com.paginass.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final BusinessProfileService businessProfileService;
    private final UserRepository userRepository;

    @PostMapping("/api/claims")
    public ResponseEntity<ClaimResponseDto> submitClaim(
            @Valid @RequestBody ClaimRequestDto request,
            Authentication authentication) {
        User claimant = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.submitClaim(request.businessId(), claimant));
    }

    @GetMapping("/api/admin/claims/pending")
    public ResponseEntity<List<ClaimResponseDto>> getPendingClaims() {
        return ResponseEntity.ok(claimService.getPendingClaims());
    }

    @PatchMapping("/api/admin/claims/{id}/resolve")
    public ResponseEntity<ClaimResponseDto> resolveClaim(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveClaimRequestDto request) {
        return ResponseEntity.ok(claimService.resolveClaim(id, request.approve()));
    }

    @DeleteMapping("/api/admin/businesses/{id}/ownership")
    public ResponseEntity<BusinessProfileDetailDto> revokeOwnership(@PathVariable UUID id) {
        claimService.revokeOwnership(id);
        return ResponseEntity.ok(businessProfileService.getProfileById(id));
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
