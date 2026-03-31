package com.company.emoji.template.dto;

public record TemplateSummaryResponse(
        String id,
        String name,
        String styleCode,
        String previewUrl,
        int priceCredits,
        boolean enabled
) {
}