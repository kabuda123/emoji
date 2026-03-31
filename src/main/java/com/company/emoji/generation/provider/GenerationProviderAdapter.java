package com.company.emoji.generation.provider;

public interface GenerationProviderAdapter {
    String providerCode();

    ProviderDispatchResponse dispatch(ProviderDispatchRequest request);
}
