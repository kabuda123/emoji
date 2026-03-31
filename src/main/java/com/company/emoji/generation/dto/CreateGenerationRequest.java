package com.company.emoji.generation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateGenerationRequest(
        @NotBlank String templateId,
        @NotBlank String inputObjectKey,
        @Min(2) @Max(4) int count
) {
}