package com.company.emoji.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, traceId, Instant.now());
    }
}