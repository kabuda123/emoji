package com.company.emoji.common.security;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.common.config.InternalApiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalApiGuard {
    private final InternalApiProperties internalApiProperties;

    public InternalApiGuard(InternalApiProperties internalApiProperties) {
        this.internalApiProperties = internalApiProperties;
    }

    public void requireValidToken(String token) {
        if (token == null || token.isBlank() || !token.equals(internalApiProperties.apiToken())) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Invalid internal API token");
        }
    }
}
