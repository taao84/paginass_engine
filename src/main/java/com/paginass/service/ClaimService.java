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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final BusinessClaimRepository businessClaimRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Autowired @Lazy
    private ClaimService self;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getPendingClaims() {
        return businessClaimRepository.findByStatus(ClaimStatus.PENDING)
                .stream()
                .map(this::enrichDto)
                .toList();
    }

    public ClaimResponseDto resolveClaim(UUID claimId, boolean approve) {
        ClaimResponseDto dto = self.resolveClaimTx(claimId, approve);
        if (approve && dto.claimantEmail() != null && !"Unknown".equals(dto.claimantEmail())) {
            String miniSiteUrl = frontendUrl + "/" + dto.businessSlug();
            mailService.sendClaimApprovedEmail(dto.claimantEmail(), dto.claimantName(), dto.businessName(), miniSiteUrl);
        }
        return dto;
    }

    @Transactional
    public ClaimResponseDto resolveClaimTx(UUID claimId, boolean approve) {
        User admin = resolveCurrentUser();
        BusinessClaim claim = businessClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found"));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This claim has already been resolved");
        }

        claim.setStatus(approve ? ClaimStatus.APPROVED : ClaimStatus.REJECTED);
        claim.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
        businessClaimRepository.save(claim);

        if (approve) {
            BusinessProfile business = businessProfileRepository.findById(claim.getBusinessId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Business not found"));
            business.setOwnerId(claim.getClaimantId());
            business.setClaimed(true);
            businessProfileRepository.save(business);
        }

        log.info("AUDIT claim_resolved claimId={} approved={} adminId={}", claimId, approve, admin.getId());
        return enrichDto(claim);
    }

    @Transactional
    public void revokeOwnership(UUID businessId) {
        User admin = resolveCurrentUser();
        BusinessProfile business = businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Business not found"));

        if (!business.isClaimed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This business is not currently claimed");
        }

        business.setOwnerId(null);
        business.setClaimed(false);
        businessProfileRepository.save(business);

        log.info("AUDIT ownership_revoked businessId={} adminId={}", businessId, admin.getId());
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud ? ud.getUsername() : auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private ClaimResponseDto enrichDto(BusinessClaim claim) {
        BusinessProfile business = businessProfileRepository.findById(claim.getBusinessId()).orElse(null);
        String businessName = business != null ? business.getName() : "Unknown";
        String businessSlug = business != null ? business.getSlug() : null;

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
