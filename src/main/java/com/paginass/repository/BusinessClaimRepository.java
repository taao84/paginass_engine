package com.paginass.repository;

import com.paginass.entity.BusinessClaim;
import com.paginass.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessClaimRepository extends JpaRepository<BusinessClaim, UUID> {

    List<BusinessClaim> findByStatus(ClaimStatus status);

    boolean existsByBusinessIdAndClaimantIdAndStatus(UUID businessId, UUID claimantId, ClaimStatus status);
}
