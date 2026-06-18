package com.paginass.repository;

import com.paginass.entity.BusinessConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessConfigurationRepository extends JpaRepository<BusinessConfiguration, UUID> {
}
