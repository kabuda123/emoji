package com.company.emoji.common.bootstrap;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BootstrapConfigService {
    private final BootstrapProperties properties;

    public BootstrapConfigService(BootstrapProperties properties) {
        this.properties = properties;
    }

    public BootstrapConfigResponse load() {
        return new BootstrapConfigResponse(
                properties.productName(),
                properties.iosReviewMode(),
                properties.iapEnabled(),
                properties.supportedLoginMethods(),
                List.of(
                        new LegalDocumentResponse("PRIVACY", "Privacy Policy", properties.privacyUrl()),
                        new LegalDocumentResponse("TERMS", "Terms of Service", properties.termsUrl()),
                        new LegalDocumentResponse("AI_AUTH", "AI Authorization", properties.aiAuthUrl())
                ),
                new GenerationPolicyResponse(
                        properties.generationMinImages(),
                        properties.generationMaxImages(),
                        properties.generationPollSeconds()
                )
        );
    }
}