package com.company.emoji.generation.provider;

import java.util.List;

public record ProviderDispatchResponse(
        String providerTaskId,
        List<String> previewUrls
) {
}
