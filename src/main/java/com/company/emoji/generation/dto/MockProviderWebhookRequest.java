package com.company.emoji.generation.dto;

import com.company.emoji.generation.domain.GenerationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MockProviderWebhookRequest(
        @NotBlank @Size(max = 128) String providerTaskId,
        @NotNull GenerationStatus status,
        @Min(0) @Max(100) Integer progressPercent,
        List<@Size(max = 255) String> previewUrls,
        List<@Size(max = 255) String> resultUrls,
        @Size(max = 500) String failedReason
) {
}
