package com.company.emoji.common.api;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
    public static ErrorResponse of(ApiErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, Map.of());
    }

    public static ErrorResponse of(ApiErrorCode code, String message, Map<String, Object> details) {
        return new ErrorResponse(code.name(), message, details);
    }
}