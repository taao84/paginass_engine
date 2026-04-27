package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordConfirmRequestDto(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword
) {}
