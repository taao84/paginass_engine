package com.paginass.service;

import com.paginass.entity.BusinessProfile;
import com.paginass.entity.User;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("bizAuth")
@RequiredArgsConstructor
public class BusinessAuthorizationService {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    public boolean isOwnerOfClaimed(UUID profileId) {
        BusinessProfile profile = businessProfileRepository.findById(profileId).orElse(null);
        if (profile == null || !profile.isClaimed()) return false;
        return profile.getOwnerId().equals(currentUserId());
    }

    public boolean isUnclaimed(UUID profileId) {
        return businessProfileRepository.findById(profileId)
                .map(p -> !p.isClaimed())
                .orElse(false);
    }

    public boolean isOwnerOrAdmin(UUID businessId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;
        return isOwnerOfClaimed(businessId);
    }

    public boolean isEditorOwnerOrAdmin(UUID businessId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;
        if (isOwnerOfClaimed(businessId)) return true;
        boolean isEditor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EDITOR"));
        return isEditor && isUnclaimed(businessId);
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
