package com.company.emoji.payment.dto;

public record CreditBalanceResponse(
        int availableCredits,
        int frozenCredits,
        String currency
) {
}