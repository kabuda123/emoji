package com.company.emoji.auth;

public record AuthenticatedUser(
        String userId,
        String provider,
        String email
) {
}
