package com.company.emoji.generation;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.common.security.ProviderWebhookGuard;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.MockProviderWebhookRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/mock")
public class MockProviderWebhookController {
    private final ProviderWebhookGuard providerWebhookGuard;
    private final MockProviderWebhookService mockProviderWebhookService;

    public MockProviderWebhookController(
            ProviderWebhookGuard providerWebhookGuard,
            MockProviderWebhookService mockProviderWebhookService
    ) {
        this.providerWebhookGuard = providerWebhookGuard;
        this.mockProviderWebhookService = mockProviderWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<GenerationDetailResponse>> receiveWebhook(
            @RequestHeader(value = "X-Provider-Token", required = false) String providerToken,
            @Valid @RequestBody MockProviderWebhookRequest request
    ) {
        providerWebhookGuard.requireValidToken(providerToken);
        return ResponseEntity.ok(ApiResponse.ok(mockProviderWebhookService.handle(request), TraceIdContext.currentTraceId()));
    }
}
