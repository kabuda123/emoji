package com.company.emoji.user.dto;

import java.time.Instant;

public record AccountCleanupExecutionResponse(
        String cleanupJobId,
        String userId,
        String status,
        int generationTasksPurged,
        int mediaAssetsMarkedDeleted,
        Instant completedAt
) {
}
