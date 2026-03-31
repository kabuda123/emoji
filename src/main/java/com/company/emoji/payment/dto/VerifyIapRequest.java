package com.company.emoji.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyIapRequest(
        @NotBlank String productId,
        @NotBlank String transactionId,
        @NotBlank String receiptData
) {
}