package com.paginass.service;

import com.paginass.dto.ClaimResponseDto;
import com.paginass.entity.BusinessClaim;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.ClaimStatus;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final BusinessClaimRepository businessClaimRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClaimResponseDto submitClaim(UUID businessId, User claimant) {
        if (claimant.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admins cannot submit ownership claims");
        }

        BusinessProfile business = businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Business not found"));

        if (business.isClaimed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This business has already been claimed");
        }

        if (businessClaimRepository.existsByBusinessIdAndClaimantIdAndStatus(
                businessId, claimant.getId(), ClaimStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a pending claim on this business");
        }

        BusinessClaim claim = new BusinessClaim();
        claim.setBusinessId(businessId);
        claim.setClaimantId(claimant.getId());

        return toDto(businessClaimRepository.save(claim), business.getName(), business.getSlug(), claimant.getFullName(), claimant.getEmail());
    }

    public List<ClaimResponseDto> getPendingClaims() {
        return businessClaimRepository.findByStatus(ClaimStatus.PENDING)
                .stream()
                .map(this::enrichDto)
                .toList();
    }

    @Transactional
    public ClaimResponseDto resolveClaim(UUID claimId, boolean approve) {
        BusinessClaim claim = businessClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found"));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This claim has already been resolved");
        }

        claim.setStatus(approve ? ClaimStatus.APPROVED : ClaimStatus.REJECTED);
        claim.setResolvedAt(OffsetDateTime.now());
        businessClaimRepository.save(claim);

        if (approve) {
            BusinessProfile business = businessProfileRepository.findById(claim.getBusinessId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Business not found"));
            business.setOwnerId(claim.getClaimantId());
            business.setClaimed(true);
            businessProfileRepository.save(business);

            User claimant = userRepository.findById(claim.getClaimantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Claimant user not found"));
            claimant.setRole(UserRole.OWNER);
            userRepository.save(claimant);
        }

        return enrichDto(claim);
    }

    @Transactional
    public void revokeOwnership(UUID businessId) {
        BusinessProfile business = businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Business not found"));

        if (!business.isClaimed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This business is not currently claimed");
        }

        UUID previousOwnerId = business.getOwnerId();

        business.setOwnerId(null);
        business.setClaimed(false);
        businessProfileRepository.save(business);

        // does this user still own any business?
        boolean stillOwnsOtherBusinesses = businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(previousOwnerId);
        if (!stillOwnsOtherBusinesses) {
            User formerOwner = userRepository.findById(previousOwnerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Owner user not found"));
            formerOwner.setRole(UserRole.CONTRIBUTOR);
            userRepository.save(formerOwner);
        }
    }

    private ClaimResponseDto enrichDto(BusinessClaim claim) {
        String businessName = businessProfileRepository.findById(claim.getBusinessId())
                .map(BusinessProfile::getName)
                .orElse("Unknown");

        String businessSlug = businessProfileRepository.findById(claim.getBusinessId())
                .map(BusinessProfile::getSlug)
                .orElse(null);

        User claimant = userRepository.findById(claim.getClaimantId()).orElse(null);
        String claimantName  = claimant != null ? claimant.getFullName() : null;
        String claimantEmail = claimant != null ? claimant.getEmail() : "Unknown";

        return toDto(claim, businessName, businessSlug, claimantName, claimantEmail);
    }

    private ClaimResponseDto toDto(BusinessClaim claim, String businessName, String businessSlug, String claimantName, String claimantEmail) {
        return new ClaimResponseDto(
                claim.getId(),
                claim.getBusinessId(),
                businessName,
                businessSlug,
                claim.getClaimantId(),
                claimantName,
                claimantEmail,
                claim.getStatus().name(),
                claim.getCreatedAt(),
                claim.getResolvedAt()
        );
    }
}
