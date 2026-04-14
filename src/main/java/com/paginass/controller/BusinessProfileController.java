package com.paginass.controller;

import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileUpdateRequestDto;
import com.paginass.dto.ImageOrderRequestDto;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.UserRepository;
import com.paginass.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
        User user = resolveUser(authentication);
        if (user.getRole() == UserRole.CONTRIBUTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contributors do not have access to Mis negocios");
        }
        return ResponseEntity.ok(businessProfileService.getProfilesByOwner(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessProfileDetailDto> getBusiness(
            @PathVariable UUID id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.getProfileByOwner(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessProfileDetailDto> updateBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessProfileUpdateRequestDto request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.updateProfile(id, user, request));
    }

    @PatchMapping("/{id}/mini-site")
    public ResponseEntity<BusinessProfileDetailDto> toggleMiniSite(
            @PathVariable UUID id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.toggleMiniSite(id, user));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BusinessProfileDetailDto> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.uploadImage(id, user, file));
    }

    @PutMapping("/{id}/images/order")
    public ResponseEntity<BusinessProfileDetailDto> reorderImages(
            @PathVariable UUID id,
            @RequestBody ImageOrderRequestDto request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.reorderImages(id, user, request.imageUrls()));
    }

    @DeleteMapping("/{id}/images/{index}")
    public ResponseEntity<BusinessProfileDetailDto> deleteImage(
            @PathVariable UUID id,
            @PathVariable int index,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(businessProfileService.deleteImage(id, user, index));
    }

    private User resolveUser(Authentication authentication) {
        String email = resolveEmail(authentication);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }
}
