package com.company.emoji.media.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadPolicyRequest(
        @NotBlank String fileName,
        @NotBlank String contentType
) {
}