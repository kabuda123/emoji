package com.company.emoji.common.security;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.common.config.MockProviderProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ProviderWebhookGuard {
    private final MockProviderProperties mockProviderProperties;

    public ProviderWebhookGuard(MockProviderProperties mockProviderProperties) {
        this.mockProviderProperties = mockProviderProperties;
    }

    public void requireValidToken(String token) {
        if (token == null || token.isBlank() || !token.equals(mockProviderProperties.webhookToken())) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Invalid provider webhook token");
        }
    }
}
