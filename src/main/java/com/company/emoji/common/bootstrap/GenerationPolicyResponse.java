package com.company.emoji.common.bootstrap;

public record GenerationPolicyResponse(
        int minImages,
        int maxImages,
        int defaultPollSeconds
) {
}