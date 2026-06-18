package com.paginass.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequestDto(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Email String replyEmail,
        @NotBlank @Size(max = 1000) String message
) {}
