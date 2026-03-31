package com.company.emoji.generation;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.generation.domain.GenerationStatus;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.InternalGenerationStatusUpdateRequest;
import com.company.emoji.generation.entity.GenerationTaskEntity;
import com.company.emoji.generation.provider.GenerationProviderAdapter;
import com.company.emoji.generation.provider.ProviderDispatchRequest;
import com.company.emoji.generation.provider.ProviderDispatchResponse;
import com.company.emoji.template.TemplateRepository;
import com.company.emoji.template.entity.StyleTemplateEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationDispatchService {
    private final GenerationTaskRepository generationTaskRepository;
    private final TemplateRepository templateRepository;
    private final GenerationProviderAdapter generationProviderAdapter;
    private final GenerationService generationService;

    public GenerationDispatchService(
            GenerationTaskRepository generationTaskRepository,
            TemplateRepository templateRepository,
            GenerationProviderAdapter generationProviderAdapter,
            GenerationService generationService
    ) {
        this.generationTaskRepository = generationTaskRepository;
        this.templateRepository = templateRepository;
        this.generationProviderAdapter = generationProviderAdapter;
        this.generationService = generationService;
    }

    @Transactional
    public GenerationDetailResponse dispatch(String taskId) {
        GenerationTaskEntity task = generationTaskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Generation task not found"));
        if (GenerationStatus.valueOf(task.getStatus()) != GenerationStatus.READY_TO_DISPATCH) {
            throw new ApiException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT, "Task is not ready to dispatch");
        }

        StyleTemplateEntity template = templateRepository.findById(task.getTemplateId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Template not found"));

        ProviderDispatchResponse response = generationProviderAdapter.dispatch(new ProviderDispatchRequest(
                task.getId(),
                template.getStyleCode(),
                task.getInputObjectKey(),
                task.getRequestedCount()
        ));

        return generationService.updateStatus(taskId, new InternalGenerationStatusUpdateRequest(
                GenerationStatus.RUNNING,
                15,
                response.providerTaskId(),
                response.previewUrls(),
                null,
                null
        ));
    }
}
