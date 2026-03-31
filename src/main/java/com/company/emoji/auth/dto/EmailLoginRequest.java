package com.company.emoji.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailLoginRequest(
        @Email @NotBlank String email,
        @NotBlank String code
) {
}