package com.company.emoji.generation;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.InternalGenerationStatusUpdateRequest;
import com.company.emoji.generation.dto.MockProviderWebhookRequest;
import com.company.emoji.generation.entity.GenerationTaskEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockProviderWebhookService {
    private final GenerationTaskRepository generationTaskRepository;
    private final GenerationService generationService;

    public MockProviderWebhookService(GenerationTaskRepository generationTaskRepository, GenerationService generationService) {
        this.generationTaskRepository = generationTaskRepository;
        this.generationService = generationService;
    }

    @Transactional
    public GenerationDetailResponse handle(MockProviderWebhookRequest request) {
        GenerationTaskEntity task = generationTaskRepository.findByProviderTaskIdAndDeletedFalse(request.providerTaskId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Provider task not found"));

        return generationService.updateStatus(task.getId(), new InternalGenerationStatusUpdateRequest(
                request.status(),
                request.progressPercent(),
                request.providerTaskId(),
                request.previewUrls(),
                request.resultUrls(),
                request.failedReason()
        ));
    }
}
