package com.paginass.repository;

import com.paginass.entity.Highlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HighlightRepository extends JpaRepository<Highlight, UUID> {

    List<Highlight> findAllByEnabledTrueOrderByCreatedAtDesc();

    List<Highlight> findAllByEnabledTrueAndExpiresAtBefore(Instant now);
}
