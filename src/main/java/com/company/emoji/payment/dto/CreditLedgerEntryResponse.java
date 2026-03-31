package com.company.emoji.payment.dto;

import java.time.Instant;

public record CreditLedgerEntryResponse(
        String entryId,
        String entryType,
        int availableDelta,
        int frozenDelta,
        int balanceAfterAvailable,
        int balanceAfterFrozen,
        String generationTaskId,
        String iapOrderId,
        String description,
        Instant createdAt
) {
}
