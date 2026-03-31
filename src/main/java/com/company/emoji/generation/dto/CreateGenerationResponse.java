package com.company.emoji.generation.dto;

import com.company.emoji.generation.domain.GenerationStatus;

public record CreateGenerationResponse(
        String taskId,
        GenerationStatus status,
        int pollAfterSeconds
) {
}