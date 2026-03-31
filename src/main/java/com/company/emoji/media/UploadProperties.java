package com.company.emoji.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record UploadProperties(
        String publicUploadBaseUrl,
        String uploadPathPrefix,
        int uploadExpiresInSeconds
) {
}