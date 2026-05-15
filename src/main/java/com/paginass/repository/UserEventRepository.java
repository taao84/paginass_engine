package com.paginass.repository;

import com.paginass.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserEventRepository extends JpaRepository<UserEvent, UUID> {
}
