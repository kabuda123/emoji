package com.company.emoji.common.bootstrap;

import java.util.List;

public record BootstrapConfigResponse(
        String productName,
        boolean iosReviewMode,
        boolean iapEnabled,
        List<String> supportedLoginMethods,
        List<LegalDocumentResponse> legalDocuments,
        GenerationPolicyResponse generation
) {
}