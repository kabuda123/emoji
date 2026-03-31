package com.company.emoji.generation.dto;

import com.company.emoji.generation.domain.GenerationStatus;

import java.time.Instant;

public record HistoryItemResponse(
        String taskId,
        String templateName,
        GenerationStatus status,
        String coverUrl,
        Instant createdAt
) {
}