package com.company.emoji.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        long emailCodeTtlSeconds,
        String fixedEmailCodeForDev
) {
}