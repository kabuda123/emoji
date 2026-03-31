package com.company.emoji.media.dto;

import java.util.Map;

public record UploadPolicyResponse(
        String objectKey,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        int expiresInSeconds
) {
}