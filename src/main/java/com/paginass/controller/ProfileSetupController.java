package com.paginass.controller;

import com.paginass.dto.AuthResponseDto;
import com.paginass.dto.ProfileSetupRequestDto;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessType;
import com.paginass.entity.User;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import com.paginass.service.LocalAuthService;
import com.paginass.util.TagNormalizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileSetupController {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final LocalAuthService localAuthService;

    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    @PostMapping("/setup")
    public ResponseEntity<AuthResponseDto> setup(
            @Valid @RequestBody ProfileSetupRequestDto request,
            Authentication authentication) {

        String email = resolveEmail(authentication);
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        String displayName = resolveDisplayName(request);

        BusinessProfile profile = new BusinessProfile();
        profile.setOwnerId(owner.getId());
        profile.setCreatedBy(owner.getId());
        profile.setName(displayName);
        profile.setSlug(localAuthService.generateSlug(displayName));
        profile.setBizType(resolveBizType(request));
        profile.setDescription(request.description());

        Map<String, String> contactInfo = new HashMap<>();
        if (request.phones() != null) contactInfo.put("phones", request.phones());
        if (request.contactHours() != null) contactInfo.put("hours", request.contactHours());
        if (request.address() != null) contactInfo.put("address", request.address());
        if (request.whatsapp() != null && !request.whatsapp().isBlank()) contactInfo.put("whatsapp", request.whatsapp());
        if (request.email() != null && !request.email().isBlank()) contactInfo.put("email", request.email());
        if (request.website() != null && !request.website().isBlank()) contactInfo.put("website", request.website());
        if (request.instagram() != null && !request.instagram().isBlank()) contactInfo.put("instagram", request.instagram());
        profile.setContactInfo(contactInfo);

        profile.setTags(TagNormalizer.normalize(request.services()));
        profile.setDeliversToHome(Boolean.TRUE.equals(request.deliversToHome()));
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());

        BusinessProfile saved = businessProfileRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDto(saved.getId(), "Profile created successfully"));
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

    private String resolveDisplayName(ProfileSetupRequestDto request) {
        if (request.accountType() == ProfileSetupRequestDto.AccountType.PERSON) {
            return request.fullName() != null ? request.fullName() : "Sin nombre";
        }
        return request.businessName() != null ? request.businessName() : "Sin nombre";
    }

    private BusinessType resolveBizType(ProfileSetupRequestDto request) {
        if (request.accountType() == ProfileSetupRequestDto.AccountType.PERSON) {
            return BusinessType.GIG;
        }
        return Boolean.TRUE.equals(request.isBrickAndMortar())
                ? BusinessType.BRICK_AND_MORTAR
                : BusinessType.INFORMAL;
    }
}
