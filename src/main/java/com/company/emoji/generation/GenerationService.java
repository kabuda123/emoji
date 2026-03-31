package com.company.emoji.generation;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.generation.domain.GenerationStatus;
import com.company.emoji.generation.dto.CreateGenerationRequest;
import com.company.emoji.generation.dto.CreateGenerationResponse;
import com.company.emoji.generation.dto.DeleteHistoryResponse;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.HistoryItemResponse;
import com.company.emoji.generation.entity.GenerationTaskEntity;
import com.company.emoji.template.TemplateRepository;
import com.company.emoji.template.entity.StyleTemplateEntity;
import com.company.emoji.user.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationService {
    private final UserAccountService userAccountService;
    private final GenerationTaskRepository generationTaskRepository;
    private final TemplateRepository templateRepository;

    public GenerationService(
            UserAccountService userAccountService,
            GenerationTaskRepository generationTaskRepository,
            TemplateRepository templateRepository
    ) {
        this.userAccountService = userAccountService;
        this.generationTaskRepository = generationTaskRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional
    public CreateGenerationResponse create(String userId, CreateGenerationRequest request, String idempotencyKey) {
        StyleTemplateEntity template = requireTemplate(request.templateId());

        if (userId != null) {
            userAccountService.requireActiveUser(userId);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                return generationTaskRepository.findFirstByUserIdAndIdempotencyKeyAndDeletedFalse(userId, idempotencyKey)
                        .map(this::toCreateResponse)
                        .orElseGet(() -> createTask(userId, template, request, idempotencyKey));
            }
        }

        return createTask(userId, template, request, idempotencyKey);
    }

    @Transactional(readOnly = true)
    public GenerationDetailResponse getDetail(String taskId) {
        GenerationTaskEntity task = generationTaskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Generation task not found"));
        return toDetailResponse(task);
    }

    @Transactional(readOnly = true)
    public List<HistoryItemResponse> listHistory(String userId) {
        userAccountService.requireActiveUser(userId);
        return generationTaskRepository.findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::toHistoryItem)
                .toList();
    }

    @Transactional
    public DeleteHistoryResponse deleteHistory(String userId, String historyId) {
        userAccountService.requireActiveUser(userId);
        GenerationTaskEntity task = generationTaskRepository.findByIdAndUserIdAndDeletedFalse(historyId, userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "History item not found"));
        task.setDeleted(true);
        task.setUpdatedAt(Instant.now());
        return new DeleteHistoryResponse(true, historyId);
    }

    private CreateGenerationResponse createTask(String userId, StyleTemplateEntity template, CreateGenerationRequest request, String idempotencyKey) {
        Instant now = Instant.now();
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setId("task_" + UUID.randomUUID().toString().replace("-", ""));
        task.setUserId(userId);
        task.setTemplateId(template.getId());
        task.setInputObjectKey(request.inputObjectKey());
        task.setRequestedCount(request.count());
        task.setStatus(GenerationStatus.CREATED.name());
        task.setProgressPercent(0);
        task.setPreviewUrls(template.getPreviewUrl());
        task.setResultUrls("");
        task.setFailedReason(null);
        task.setIdempotencyKey(blankToNull(idempotencyKey));
        task.setDeleted(false);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        generationTaskRepository.save(task);
        return toCreateResponse(task);
    }

    private CreateGenerationResponse toCreateResponse(GenerationTaskEntity task) {
        return new CreateGenerationResponse(task.getId(), GenerationStatus.valueOf(task.getStatus()), 5);
    }

    private GenerationDetailResponse toDetailResponse(GenerationTaskEntity task) {
        return new GenerationDetailResponse(
                task.getId(),
                GenerationStatus.valueOf(task.getStatus()),
                task.getProgressPercent(),
                splitCsv(task.getPreviewUrls()),
                splitCsv(task.getResultUrls()),
                task.getFailedReason()
        );
    }

    private HistoryItemResponse toHistoryItem(GenerationTaskEntity task) {
        StyleTemplateEntity template = requireTemplate(task.getTemplateId());
        return new HistoryItemResponse(
                task.getId(),
                template.getName(),
                GenerationStatus.valueOf(task.getStatus()),
                historyCoverUrl(task, template),
                task.getCreatedAt()
        );
    }

    private String historyCoverUrl(GenerationTaskEntity task, StyleTemplateEntity template) {
        List<String> resultUrls = splitCsv(task.getResultUrls());
        if (!resultUrls.isEmpty()) {
            return resultUrls.get(0);
        }
        List<String> previewUrls = splitCsv(task.getPreviewUrls());
        if (!previewUrls.isEmpty()) {
            return previewUrls.get(0);
        }
        return template.getPreviewUrl();
    }

    private StyleTemplateEntity requireTemplate(String templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Template not found"));
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
