package com.company.emoji.auth.dto;

public record EmailSendCodeResponse(
        int cooldownSeconds,
        String maskedDestination
) {
}