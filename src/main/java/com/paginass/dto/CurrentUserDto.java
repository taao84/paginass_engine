package com.paginass.dto;

import com.paginass.entity.UserRole;

public record CurrentUserDto(String name, String email, UserRole role, boolean hasOwnedBusiness) {}
