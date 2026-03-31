package com.company.emoji.common.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
        String productName,
        boolean iosReviewMode,
        boolean iapEnabled,
        List<String> supportedLoginMethods,
        String privacyUrl,
        String termsUrl,
        String aiAuthUrl,
        int generationMinImages,
        int generationMaxImages,
        int generationPollSeconds
) {
}