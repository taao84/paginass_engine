package com.paginass.service;

import com.paginass.entity.BusinessClaim;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.ClaimStatus;
import com.paginass.entity.User;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock private BusinessClaimRepository businessClaimRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ClaimService claimService;

    @Test
    void resolveClaim_setsResolvedAtInUTC() {
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

        when(businessClaimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(businessClaimRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(userRepository.findById(claimantId)).thenReturn(Optional.of(claimant));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        claimService.resolveClaim(claimId, true);

        assertThat(claim.getResolvedAt()).isNotNull();
        assertThat(claim.getResolvedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void resolveClaim_rejection_setsResolvedAtInUTC() {
        UUID claimId = UUID.randomUUID();

        BusinessClaim claim = new BusinessClaim();
        claim.setBusinessId(UUID.randomUUID());
        claim.setClaimantId(UUID.randomUUID());
        claim.setStatus(ClaimStatus.PENDING);

        User claimant = new User();
        claimant.setFullName("Test User");
        claimant.setEmail("test@example.com");

        when(businessClaimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(businessClaimRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(claim.getClaimantId())).thenReturn(Optional.of(claimant));

        claimService.resolveClaim(claimId, false);

        assertThat(claim.getResolvedAt()).isNotNull();
        assertThat(claim.getResolvedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
