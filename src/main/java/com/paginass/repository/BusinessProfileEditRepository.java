package com.paginass.repository;

import com.paginass.entity.BusinessProfileEdit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessProfileEditRepository extends JpaRepository<BusinessProfileEdit, UUID> {
    List<BusinessProfileEdit> findByBusinessProfileIdOrderByEditedAtDesc(UUID businessProfileId);
}
