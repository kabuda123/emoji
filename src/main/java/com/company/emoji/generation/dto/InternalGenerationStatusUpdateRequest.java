package com.company.emoji.generation.dto;

import com.company.emoji.generation.domain.GenerationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InternalGenerationStatusUpdateRequest(
        @NotNull GenerationStatus status,
        @Min(0) @Max(100) Integer progressPercent,
        @Size(max = 128) String providerTaskId,
        List<@Size(max = 255) String> previewUrls,
        List<@Size(max = 255) String> resultUrls,
        @Size(max = 500) String failedReason
) {
}
