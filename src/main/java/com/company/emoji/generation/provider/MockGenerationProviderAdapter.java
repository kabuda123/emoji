package com.company.emoji.generation.provider;

import com.company.emoji.common.config.MockProviderProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockGenerationProviderAdapter implements GenerationProviderAdapter {
    private final MockProviderProperties mockProviderProperties;

    public MockGenerationProviderAdapter(MockProviderProperties mockProviderProperties) {
        this.mockProviderProperties = mockProviderProperties;
    }

    @Override
    public String providerCode() {
        return "MOCK";
    }

    @Override
    public ProviderDispatchResponse dispatch(ProviderDispatchRequest request) {
        String providerTaskId = "mock_" + request.taskId();
        return new ProviderDispatchResponse(
                providerTaskId,
                List.of(mockProviderProperties.previewBaseUrl() + "/" + providerTaskId + ".png")
        );
    }
}
