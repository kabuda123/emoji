package com.company.emoji.generation.provider;

public record ProviderDispatchRequest(
        String taskId,
        String templateStyleCode,
        String inputObjectKey,
        int count
) {
}
