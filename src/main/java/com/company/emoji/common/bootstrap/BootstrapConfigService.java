package com.company.emoji.common.bootstrap;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BootstrapConfigService {
    private final BootstrapProperties properties;
    private final AtomicReference<BootstrapConfigAdminUpdateRequest> overrideRef = new AtomicReference<>();

    public BootstrapConfigService(BootstrapProperties properties) {
        this.properties = properties;
    }

    public BootstrapConfigResponse load() {
        BootstrapConfigAdminUpdateRequest override = overrideRef.get();
        return new BootstrapConfigResponse(
                stringValue(override != null ? override.productName() : null, properties.productName()),
                booleanValue(override != null ? override.iosReviewMode() : null, properties.iosReviewMode()),
                booleanValue(override != null ? override.iapEnabled() : null, properties.iapEnabled()),
                listValue(override != null ? override.supportedLoginMethods() : null, properties.supportedLoginMethods()),
                List.of(
                        new LegalDocumentResponse("PRIVACY", "Privacy Policy", stringValue(override != null ? override.privacyUrl() : null, properties.privacyUrl())),
                        new LegalDocumentResponse("TERMS", "Terms of Service", stringValue(override != null ? override.termsUrl() : null, properties.termsUrl())),
                        new LegalDocumentResponse("AI_AUTH", "AI Authorization", stringValue(override != null ? override.aiAuthUrl() : null, properties.aiAuthUrl()))
                ),
                new GenerationPolicyResponse(
                        intValue(override != null ? override.generationMinImages() : null, properties.generationMinImages()),
                        intValue(override != null ? override.generationMaxImages() : null, properties.generationMaxImages()),
                        intValue(override != null ? override.generationPollSeconds() : null, properties.generationPollSeconds())
                )
        );
    }

    public BootstrapConfigResponse update(BootstrapConfigAdminUpdateRequest request) {
        overrideRef.set(request);
        return load();
    }

    private String stringValue(String override, String fallback) {
        return override == null || override.isBlank() ? fallback : override;
    }

    private boolean booleanValue(Boolean override, boolean fallback) {
        return override == null ? fallback : override;
    }

    private int intValue(Integer override, int fallback) {
        return override == null ? fallback : override;
    }

    private List<String> listValue(List<String> override, List<String> fallback) {
        return override == null || override.isEmpty() ? fallback : override;
    }
}
