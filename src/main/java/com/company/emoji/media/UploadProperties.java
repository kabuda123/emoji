package com.company.emoji.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.media")
public record UploadProperties(
        String publicUploadBaseUrl,
        String uploadPathPrefix,
        String sourcePathPrefix,
        String previewPathPrefix,
        String resultPathPrefix,
        List<String> allowedContentTypes,
        int uploadExpiresInSeconds
) {
}
