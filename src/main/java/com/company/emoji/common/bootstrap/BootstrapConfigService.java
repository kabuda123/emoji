package com.company.emoji.common.bootstrap;

import com.company.emoji.audit.AuditEventService;
import com.company.emoji.common.bootstrap.entity.BootstrapConfigOverrideEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class BootstrapConfigService {
    private static final String OVERRIDE_ID = "bootstrap";

    private final BootstrapProperties properties;
    private final BootstrapConfigOverrideRepository overrideRepository;
    private final AuditEventService auditEventService;

    public BootstrapConfigService(
            BootstrapProperties properties,
            BootstrapConfigOverrideRepository overrideRepository,
            AuditEventService auditEventService
    ) {
        this.properties = properties;
        this.overrideRepository = overrideRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public BootstrapConfigResponse load() {
        BootstrapConfigOverrideEntity override = overrideRepository.findById(OVERRIDE_ID).orElse(null);
        return new BootstrapConfigResponse(
                stringValue(override != null ? override.getProductName() : null, properties.productName()),
                booleanValue(override != null ? override.getIosReviewMode() : null, properties.iosReviewMode()),
                booleanValue(override != null ? override.getIapEnabled() : null, properties.iapEnabled()),
                listValue(override != null ? splitCsv(override.getSupportedLoginMethods()) : null, properties.supportedLoginMethods()),
                List.of(
                        new LegalDocumentResponse("PRIVACY", "Privacy Policy", stringValue(override != null ? override.getPrivacyUrl() : null, properties.privacyUrl())),
                        new LegalDocumentResponse("TERMS", "Terms of Service", stringValue(override != null ? override.getTermsUrl() : null, properties.termsUrl())),
                        new LegalDocumentResponse("AI_AUTH", "AI Authorization", stringValue(override != null ? override.getAiAuthUrl() : null, properties.aiAuthUrl()))
                ),
                new GenerationPolicyResponse(
                        intValue(override != null ? override.getGenerationMinImages() : null, properties.generationMinImages()),
                        intValue(override != null ? override.getGenerationMaxImages() : null, properties.generationMaxImages()),
                        intValue(override != null ? override.getGenerationPollSeconds() : null, properties.generationPollSeconds())
                )
        );
    }

    @Transactional
    public BootstrapConfigResponse update(BootstrapConfigAdminUpdateRequest request) {
        BootstrapConfigOverrideEntity override = overrideRepository.findById(OVERRIDE_ID)
                .orElseGet(BootstrapConfigOverrideEntity::new);
        Instant now = Instant.now();
        if (override.getId() == null) {
            override.setId(OVERRIDE_ID);
            override.setCreatedAt(now);
        }
        override.setProductName(normalizeString(request.productName()));
        override.setIosReviewMode(request.iosReviewMode());
        override.setIapEnabled(request.iapEnabled());
        override.setSupportedLoginMethods(joinCsv(request.supportedLoginMethods()));
        override.setPrivacyUrl(normalizeString(request.privacyUrl()));
        override.setTermsUrl(normalizeString(request.termsUrl()));
        override.setAiAuthUrl(normalizeString(request.aiAuthUrl()));
        override.setGenerationMinImages(request.generationMinImages());
        override.setGenerationMaxImages(request.generationMaxImages());
        override.setGenerationPollSeconds(request.generationPollSeconds());
        override.setUpdatedAt(now);
        overrideRepository.save(override);
        auditEventService.recordAdmin(
                "BOOTSTRAP_CONFIG_UPDATED",
                "SYSTEM",
                "bootstrapOverrideId=" + OVERRIDE_ID
        );
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

    private String normalizeString(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }
}
