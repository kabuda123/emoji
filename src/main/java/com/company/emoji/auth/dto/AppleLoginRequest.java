package com.company.emoji.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
        @NotBlank String identityToken,
        String authorizationCode
) {
}