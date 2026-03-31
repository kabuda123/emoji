package com.company.emoji.common.api;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdContext {
    public static final String TRACE_ID_KEY = "traceId";

    private TraceIdContext() {
    }

    public static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "");
    }
}