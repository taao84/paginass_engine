package com.paginass.service;

import com.paginass.entity.Advertisement;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertisementAuthorizationServiceTest {

    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AdvertisementAuthorizationService adAuth;

    private User owner;
    private User admin;
    private User stranger;
    private Advertisement ad;
    private UUID adId;

    @BeforeEach
    void setUp() {
        owner = buildUser("owner@test.com", UserRole.CONTRIBUTOR);
        admin = buildUser("admin@test.com", UserRole.ADMIN);
        stranger = buildUser("stranger@test.com", UserRole.CONTRIBUTOR);

        adId = UUID.randomUUID();
        ad = new Advertisement();
        ad.setId(adId);
        ad.setOwnerId(owner.getId());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // --- isOwner ---

    @Test
    void isOwner_ownerReturnsTrue() {
        authenticateAs(owner);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.of(ad));
        assertThat(adAuth.isOwner(adId)).isTrue();
    }

    @Test
    void isOwner_strangerReturnsFalse() {
        authenticateAs(stranger);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.of(ad));
        assertThat(adAuth.isOwner(adId)).isFalse();
    }

    @Test
    void isOwner_nonExistentAdReturnsFalse() {
        authenticateAs(owner);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.empty());
        assertThat(adAuth.isOwner(adId)).isFalse();
    }

    // --- isOwnerOrAdmin ---

    @Test
    void isOwnerOrAdmin_ownerReturnsTrue() {
        authenticateAs(owner);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.of(ad));
        assertThat(adAuth.isOwnerOrAdmin(adId)).isTrue();
    }

    @Test
    void isOwnerOrAdmin_adminReturnsTrue() {
        authenticateAs(admin);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.of(ad));
        assertThat(adAuth.isOwnerOrAdmin(adId)).isTrue();
    }

    @Test
    void isOwnerOrAdmin_strangerReturnsFalse() {
        authenticateAs(stranger);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.of(ad));
        assertThat(adAuth.isOwnerOrAdmin(adId)).isFalse();
    }

    @Test
    void isOwnerOrAdmin_nonExistentAdReturnsFalse() {
        authenticateAs(admin);
        when(advertisementRepository.findById(adId)).thenReturn(Optional.empty());
        assertThat(adAuth.isOwnerOrAdmin(adId)).isFalse();
    }

    // --- Helpers ---

    private User buildUser(String email, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private void authenticateAs(User user) {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }
}
