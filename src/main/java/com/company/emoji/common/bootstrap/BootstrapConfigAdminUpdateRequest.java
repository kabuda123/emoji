package com.company.emoji.common.bootstrap;

import jakarta.validation.constraints.Min;

import java.util.List;

public record BootstrapConfigAdminUpdateRequest(
        String productName,
        Boolean iosReviewMode,
        Boolean iapEnabled,
        List<String> supportedLoginMethods,
        String privacyUrl,
        String termsUrl,
        String aiAuthUrl,
        @Min(1) Integer generationMinImages,
        @Min(1) Integer generationMaxImages,
        @Min(1) Integer generationPollSeconds
) {
}
