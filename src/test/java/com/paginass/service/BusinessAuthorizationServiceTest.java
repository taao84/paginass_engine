package com.paginass.service;

import com.paginass.entity.BusinessProfile;
import com.paginass.entity.User;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessAuthorizationServiceTest {

    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BusinessAuthorizationService bizAuth;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // isOwnerOfClaimed
    // -------------------------------------------------------------------------

    @Test
    void isOwnerOfClaimed_returnsTrueWhenCurrentUserOwnsClaimedProfile() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        setupSecurityContext("owner@example.com");

        BusinessProfile profile = claimedProfile(profileId, userId);
        User user = userWithEmail(userId, "owner@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        assertThat(bizAuth.isOwnerOfClaimed(profileId)).isTrue();
    }

    @Test
    void isOwnerOfClaimed_returnsFalseWhenProfileOwnedByDifferentUser() {
        UUID profileId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID actualOwnerId = UUID.randomUUID();

        setupSecurityContext("other@example.com");

        BusinessProfile profile = claimedProfile(profileId, actualOwnerId);
        User currentUser = userWithEmail(currentUserId, "other@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(currentUser));

        assertThat(bizAuth.isOwnerOfClaimed(profileId)).isFalse();
    }

    @Test
    void isOwnerOfClaimed_returnsFalseWhenProfileIsUnclaimed() {
        UUID profileId = UUID.randomUUID();

        setupSecurityContext("user@example.com");

        BusinessProfile profile = new BusinessProfile();
        profile.setId(profileId);
        profile.setClaimed(false);

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThat(bizAuth.isOwnerOfClaimed(profileId)).isFalse();
    }

    @Test
    void isOwnerOfClaimed_returnsFalseWhenProfileNotFound() {
        UUID profileId = UUID.randomUUID();
        setupSecurityContext("user@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThat(bizAuth.isOwnerOfClaimed(profileId)).isFalse();
    }

    // -------------------------------------------------------------------------
    // isUnclaimed
    // -------------------------------------------------------------------------

    @Test
    void isUnclaimed_returnsTrueWhenProfileIsNotClaimed() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = new BusinessProfile();
        profile.setId(profileId);
        profile.setClaimed(false);

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThat(bizAuth.isUnclaimed(profileId)).isTrue();
    }

    @Test
    void isUnclaimed_returnsFalseWhenProfileIsClaimed() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = claimedProfile(profileId, UUID.randomUUID());

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThat(bizAuth.isUnclaimed(profileId)).isFalse();
    }

    @Test
    void isUnclaimed_returnsFalseWhenProfileNotFound() {
        UUID profileId = UUID.randomUUID();
        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThat(bizAuth.isUnclaimed(profileId)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setupSecurityContext(String email) {
        var auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        email, "", List.of(new SimpleGrantedAuthority("ROLE_OWNER"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private BusinessProfile claimedProfile(UUID profileId, UUID ownerId) {
        BusinessProfile p = new BusinessProfile();
        p.setId(profileId);
        p.setClaimed(true);
        p.setOwnerId(ownerId);
        return p;
    }

    private User userWithEmail(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }
}
