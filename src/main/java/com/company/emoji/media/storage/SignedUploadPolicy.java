package com.company.emoji.media.storage;

import java.util.Map;

public record SignedUploadPolicy(
        String uploadUrl,
        String method,
        Map<String, String> headers
) {
}
