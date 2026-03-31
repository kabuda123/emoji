package com.company.emoji.common.security;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ApiSecurityResponseWriter responseWriter;

    public ApiAuthenticationEntryPoint(ApiSecurityResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        responseWriter.write(response, HttpStatus.UNAUTHORIZED.value(), ErrorResponse.of(ApiErrorCode.UNAUTHORIZED, authException.getMessage()));
    }
}
