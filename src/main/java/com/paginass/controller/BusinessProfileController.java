package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileUpdateRequestDto;
import com.paginass.dto.ImageOrderRequestDto;
import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import com.paginass.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile/businesses")
@RequiredArgsConstructor
public class BusinessProfileController {

    private final BusinessProfileService businessProfileService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BusinessProfileDetailDto>> listMyBusinesses(Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.getProfilesByOwner(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessProfileDetailDto> getBusiness(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.getProfileByOwner(id, ownerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessProfileDetailDto> updateBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessProfileUpdateRequestDto request,
            Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.updateProfile(id, ownerId, request));
    }

    @PatchMapping("/{id}/mini-site")
    public ResponseEntity<BusinessProfileDetailDto> toggleMiniSite(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.toggleMiniSite(id, ownerId));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BusinessProfileDetailDto> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.uploadImage(id, ownerId, file));
    }

    @PutMapping("/{id}/images/order")
    public ResponseEntity<BusinessProfileDetailDto> reorderImages(
            @PathVariable UUID id,
            @RequestBody ImageOrderRequestDto request,
            Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.reorderImages(id, ownerId, request.imageUrls()));
    }

    @DeleteMapping("/{id}/images/{index}")
    public ResponseEntity<BusinessProfileDetailDto> deleteImage(
            @PathVariable UUID id,
            @PathVariable int index,
            Authentication authentication) {
        UUID ownerId = resolveOwnerId(authentication);
        return ResponseEntity.ok(businessProfileService.deleteImage(id, ownerId, index));
    }

    private UUID resolveOwnerId(Authentication authentication) {
        String email = resolveEmail(authentication);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
        return user.getId();
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }
}
