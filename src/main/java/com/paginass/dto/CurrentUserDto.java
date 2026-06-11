package com.paginass.dto;

import com.paginass.entity.UserRole;

import java.util.UUID;

public record CurrentUserDto(UUID id, String name, String email, UserRole role, boolean hasOwnedBusiness) {}
