package com.paginass.repository;

import com.paginass.entity.ContactSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactSettingsRepository extends JpaRepository<ContactSettings, Integer> {
}
