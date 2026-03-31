package com.company.emoji.payment.dto;

public record VerifyIapResponse(
        String orderId,
        String status,
        int creditsGranted,
        int balanceAfter
) {
}