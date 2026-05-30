package com.paginass.service;

import com.paginass.entity.BusinessClaim;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.ClaimStatus;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock private BusinessClaimRepository businessClaimRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private MailService mailService;
    @Mock private SettingsService settingsService;

    @InjectMocks
    private ClaimService claimService;

    private User admin;

    @BeforeEach
    void setupSecurityContext() {
        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        ReflectionTestUtils.setField(claimService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(claimService, "self", claimService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveClaim_approval_setsResolvedAtInUTC() {
        UUID claimId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID claimantId = UUID.randomUUID();

        BusinessClaim claim = new BusinessClaim();
        claim.setBusinessId(businessId);
        claim.setClaimantId(claimantId);
        claim.setStatus(ClaimStatus.PENDING);

        BusinessProfile business = new BusinessProfile();
        business.setId(businessId);
        business.setName("Test Business");
        business.setSlug("test-business");

        User claimant = new User();
        claimant.setFullName("Test User");
        claimant.setEmail("test@example.com");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(claimantId)).thenReturn(Optional.of(claimant));
        when(businessClaimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(businessClaimRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        claimService.resolveClaim(claimId, true);

        assertThat(claim.getResolvedAt()).isNotNull();
        assertThat(claim.getResolvedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(business.isClaimed()).isTrue();
        assertThat(business.getOwnerId()).isEqualTo(claimantId);
    }

    @Test
    void resolveClaim_approval_sendsEmailToClaimant() {
        UUID claimId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID claimantId = UUID.randomUUID();

        BusinessClaim claim = new BusinessClaim();
        claim.setBusinessId(businessId);
        claim.setClaimantId(claimantId);
        claim.setStatus(ClaimStatus.PENDING);

        BusinessProfile business = new BusinessProfile();
        business.setId(businessId);
        business.setName("Test Business");
        business.setSlug("test-business");

        User claimant = new User();
        claimant.setFullName("Test User");
        claimant.setEmail("test@example.com");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(claimantId)).thenReturn(Optional.of(claimant));
        when(businessClaimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(businessClaimRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        claimService.resolveClaim(claimId, true);

        verify(mailService).sendClaimApprovedEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq("Test Business"),
                eq("http://localhost:3000/test-business")
        );
    }

    @Test
    void resolveClaim_rejection_setsResolvedAtInUTC() {
        UUID claimId = UUID.randomUUID();

        BusinessClaim claim = new BusinessClaim();
        claim.setBusinessId(UUID.randomUUID());
        claim.setClaimantId(UUID.randomUUID());
        claim.setStatus(ClaimStatus.PENDING);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(businessClaimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(businessClaimRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        claimService.resolveClaim(claimId, false);

        assertThat(claim.getResolvedAt()).isNotNull();
        assertThat(claim.getResolvedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        verify(mailService, never()).sendClaimApprovedEmail(any(), any(), any(), any());
    }

    @Test
    void submitClaim_withEmailConfigured_sendsAdminNotification() {
        UUID businessId = UUID.randomUUID();

        BusinessProfile business = new BusinessProfile();
        business.setId(businessId);
        business.setName("Mi Negocio");
        business.setSlug("mi-negocio");
        business.setClaimed(false);

        User claimant = new User();
        claimant.setId(UUID.randomUUID());
        claimant.setFullName("Juan Pérez");
        claimant.setEmail("juan@example.com");
        claimant.setRole(UserRole.CONTRIBUTOR);

        BusinessClaim savedClaim = new BusinessClaim();
        savedClaim.setBusinessId(businessId);
        savedClaim.setClaimantId(claimant.getId());
        savedClaim.setStatus(ClaimStatus.PENDING);

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessClaimRepository.existsByBusinessIdAndClaimantIdAndStatus(
                eq(businessId), eq(claimant.getId()), eq(ClaimStatus.PENDING))).thenReturn(false);
        when(businessClaimRepository.save(any())).thenReturn(savedClaim);
        when(userRepository.findById(claimant.getId())).thenReturn(Optional.of(claimant));
        when(settingsService.getAdminNotificationEmail()).thenReturn(Optional.of("admin@paginass.com"));

        claimService.submitClaim(businessId, claimant);

        verify(mailService).sendNewClaimNotificationEmail(
                eq("admin@paginass.com"),
                eq("Juan Pérez"),
                eq("juan@example.com"),
                eq("Mi Negocio"),
                eq("http://localhost:3000/admin/claims")
        );
    }

    @Test
    void submitClaim_withNoEmailConfigured_skipsAdminNotification() {
        UUID businessId = UUID.randomUUID();

        BusinessProfile business = new BusinessProfile();
        business.setId(businessId);
        business.setName("Mi Negocio");
        business.setSlug("mi-negocio");
        business.setClaimed(false);

        User claimant = new User();
        claimant.setId(UUID.randomUUID());
        claimant.setFullName("Juan Pérez");
        claimant.setEmail("juan@example.com");
        claimant.setRole(UserRole.CONTRIBUTOR);

        BusinessClaim savedClaim = new BusinessClaim();
        savedClaim.setBusinessId(businessId);
        savedClaim.setClaimantId(claimant.getId());
        savedClaim.setStatus(ClaimStatus.PENDING);

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessClaimRepository.existsByBusinessIdAndClaimantIdAndStatus(
                eq(businessId), eq(claimant.getId()), eq(ClaimStatus.PENDING))).thenReturn(false);
        when(businessClaimRepository.save(any())).thenReturn(savedClaim);
        when(userRepository.findById(claimant.getId())).thenReturn(Optional.of(claimant));
        when(settingsService.getAdminNotificationEmail()).thenReturn(Optional.empty());

        claimService.submitClaim(businessId, claimant);

        verify(mailService, never()).sendNewClaimNotificationEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void revokeOwnership_clearsOwnerAndClaimedStatus() {
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BusinessProfile business = new BusinessProfile();
        business.setId(businessId);
        business.setClaimed(true);
        business.setOwnerId(ownerId);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        claimService.revokeOwnership(businessId);

        assertThat(business.isClaimed()).isFalse();
        assertThat(business.getOwnerId()).isNull();
    }
}
