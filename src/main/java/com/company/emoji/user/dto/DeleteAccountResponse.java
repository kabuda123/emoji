package com.company.emoji.user.dto;

import java.time.Instant;

public record DeleteAccountResponse(
        String status,
        Instant scheduledDeletionAt,
        String cleanupJobId
) {
}
