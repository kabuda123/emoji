package com.company.emoji.template.dto;

import jakarta.validation.constraints.Min;

public record InternalTemplateUpdateRequest(
        Boolean enabled,
        @Min(0) Integer priceCredits
) {
}
