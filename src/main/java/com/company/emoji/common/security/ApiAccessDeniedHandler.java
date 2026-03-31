package com.company.emoji.common.security;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final ApiSecurityResponseWriter responseWriter;

    public ApiAccessDeniedHandler(ApiSecurityResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        responseWriter.write(response, HttpStatus.FORBIDDEN.value(), ErrorResponse.of(ApiErrorCode.FORBIDDEN, "Access denied"));
    }
}
