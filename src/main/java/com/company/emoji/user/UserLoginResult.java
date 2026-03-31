package com.company.emoji.user;

public record UserLoginResult(
        String userId,
        boolean isNewUser
) {
}
