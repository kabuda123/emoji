package com.company.emoji.auth.dto;

public record AuthSessionResponse(
        String userId,
        String accessToken,
        String refreshToken,
        long expiresIn,
        boolean isNewUser
) {
}