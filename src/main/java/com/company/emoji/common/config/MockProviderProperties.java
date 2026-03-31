package com.company.emoji.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.provider.mock")
public record MockProviderProperties(
        String webhookToken,
        String previewBaseUrl,
        String resultBaseUrl
) {
}
