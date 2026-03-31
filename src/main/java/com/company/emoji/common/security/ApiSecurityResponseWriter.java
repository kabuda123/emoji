package com.company.emoji.common.security;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.ErrorResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiSecurityResponseWriter {
    private final ObjectMapper objectMapper;

    public ApiSecurityResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, int status, ErrorResponse error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(error, TraceIdContext.currentTraceId()));
    }
}
