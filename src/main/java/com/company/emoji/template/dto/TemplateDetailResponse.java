package com.company.emoji.template.dto;

import java.util.List;

public record TemplateDetailResponse(
        String id,
        String name,
        String styleCode,
        String description,
        String previewUrl,
        List<String> sampleImages,
        int priceCredits,
        boolean enabled,
        List<String> supportedAspectRatios
) {
}