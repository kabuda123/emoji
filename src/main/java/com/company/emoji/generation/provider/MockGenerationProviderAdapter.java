package com.company.emoji.generation.provider;

import com.company.emoji.media.MediaAssetService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockGenerationProviderAdapter implements GenerationProviderAdapter {
    private final MediaAssetService mediaAssetService;

    public MockGenerationProviderAdapter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
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
                List.of(mediaAssetService.buildPreviewObjectKey(providerTaskId, 1))
        );
    }
}
