package com.company.emoji.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, null, traceId, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorResponse error, String traceId) {
        return new ApiResponse<>(false, null, error, traceId, Instant.now());
    }
}