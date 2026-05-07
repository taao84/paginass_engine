package com.paginass.service;

import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("adAuth")
@RequiredArgsConstructor
public class AdvertisementAuthorizationService {

    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;

    public boolean isOwner(UUID adId) {
        UUID userId = currentUser().getId();
        return advertisementRepository.findById(adId)
                .map(ad -> ad.getOwnerId().equals(userId))
                .orElse(false);
    }

    public boolean isOwnerOrAdmin(UUID adId) {
        User user = currentUser();
        return advertisementRepository.findById(adId)
                .map(ad -> ad.getOwnerId().equals(user.getId()) || user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
