package com.company.emoji.generation;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.generation.domain.GenerationCreditStatus;
import com.company.emoji.generation.domain.GenerationStatus;
import com.company.emoji.generation.dto.CreateGenerationRequest;
import com.company.emoji.generation.dto.CreateGenerationResponse;
import com.company.emoji.generation.dto.DeleteHistoryResponse;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.HistoryItemResponse;
import com.company.emoji.generation.dto.InternalGenerationStatusUpdateRequest;
import com.company.emoji.generation.entity.GenerationTaskEntity;
import com.company.emoji.media.MediaAssetService;
import com.company.emoji.template.TemplateRepository;
import com.company.emoji.template.entity.StyleTemplateEntity;
import com.company.emoji.user.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationService {
    private final UserAccountService userAccountService;
    private final GenerationTaskRepository generationTaskRepository;
    private final MediaAssetService mediaAssetService;
    private final TemplateRepository templateRepository;

    public GenerationService(
            UserAccountService userAccountService,
            GenerationTaskRepository generationTaskRepository,
            MediaAssetService mediaAssetService,
            TemplateRepository templateRepository
    ) {
        this.userAccountService = userAccountService;
        this.generationTaskRepository = generationTaskRepository;
        this.mediaAssetService = mediaAssetService;
        this.templateRepository = templateRepository;
    }

    @Transactional
    public CreateGenerationResponse create(String userId, CreateGenerationRequest request, String idempotencyKey) {
        StyleTemplateEntity template = requireTemplate(request.templateId());
        mediaAssetService.assertValidSourceObjectKey(request.inputObjectKey());

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

    @Transactional
    public GenerationDetailResponse updateStatus(String taskId, InternalGenerationStatusUpdateRequest request) {
        GenerationTaskEntity task = generationTaskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Generation task not found"));

        GenerationStatus currentStatus = GenerationStatus.valueOf(task.getStatus());
        GenerationStatus targetStatus = request.status();
        validateTransition(currentStatus, targetStatus);

        task.setStatus(targetStatus.name());
        task.setUpdatedAt(Instant.now());

        if (request.providerTaskId() != null && !request.providerTaskId().isBlank()) {
            task.setProviderTaskId(request.providerTaskId());
        }
        if (request.previewUrls() != null) {
            task.setPreviewUrls(joinCsv(request.previewUrls()));
        }

        switch (targetStatus) {
            case AUDITING -> task.setProgressPercent(5);
            case READY_TO_DISPATCH -> task.setProgressPercent(10);
            case RUNNING -> {
                requireProviderTaskId(task);
                task.setProgressPercent(normalizeProgress(request.progressPercent(), 15, 90));
                task.setFailedReason(null);
            }
            case POST_PROCESSING -> task.setProgressPercent(normalizeProgress(request.progressPercent(), 90, 99));
            case SUCCESS -> {
                if (request.resultUrls() == null || request.resultUrls().isEmpty()) {
                    throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "resultUrls are required for SUCCESS");
                }
                settleCreditsOnSuccess(task);
                task.setProgressPercent(100);
                task.setResultUrls(joinCsv(request.resultUrls()));
                task.setFailedReason(null);
            }
            case FAILED -> {
                if (request.failedReason() == null || request.failedReason().isBlank()) {
                    throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "failedReason is required for FAILED");
                }
                releaseCreditsOnFailure(task);
                task.setFailedReason(request.failedReason().trim());
                task.setProgressPercent(normalizeProgress(request.progressPercent(), task.getProgressPercent(), 99));
            }
            case REFUNDED -> {
                refundCredits(task);
                task.setProgressPercent(100);
            }
            case CREATED -> throw new ApiException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT, "Internal status update cannot move task back to CREATED");
        }

        if (targetStatus != GenerationStatus.SUCCESS && request.resultUrls() != null) {
            task.setResultUrls(joinCsv(request.resultUrls()));
        }

        return toDetailResponse(task);
    }

    private CreateGenerationResponse createTask(String userId, StyleTemplateEntity template, CreateGenerationRequest request, String idempotencyKey) {
        Instant now = Instant.now();
        int reservedCredits = 0;
        GenerationCreditStatus creditStatus = GenerationCreditStatus.NONE;
        if (userId != null) {
            reservedCredits = template.getPriceCredits();
            userAccountService.reserveCredits(userId, reservedCredits);
            creditStatus = GenerationCreditStatus.RESERVED;
        }

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
        task.setProviderTaskId(null);
        task.setReservedCredits(reservedCredits);
        task.setCreditStatus(creditStatus.name());
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
                mediaAssetService.toPublicUrls(splitCsv(task.getPreviewUrls())),
                mediaAssetService.toPublicUrls(splitCsv(task.getResultUrls())),
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
        List<String> resultUrls = mediaAssetService.toPublicUrls(splitCsv(task.getResultUrls()));
        if (!resultUrls.isEmpty()) {
            return resultUrls.get(0);
        }
        List<String> previewUrls = mediaAssetService.toPublicUrls(splitCsv(task.getPreviewUrls()));
        if (!previewUrls.isEmpty()) {
            return previewUrls.get(0);
        }
        return mediaAssetService.toPublicUrl(template.getPreviewUrl());
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

    private void settleCreditsOnSuccess(GenerationTaskEntity task) {
        if (task.getUserId() == null || task.getReservedCredits() <= 0) {
            return;
        }
        GenerationCreditStatus creditStatus = GenerationCreditStatus.valueOf(task.getCreditStatus());
        if (creditStatus == GenerationCreditStatus.RESERVED) {
            userAccountService.consumeReservedCredits(task.getUserId(), task.getReservedCredits());
            task.setCreditStatus(GenerationCreditStatus.CONSUMED.name());
        }
    }

    private void releaseCreditsOnFailure(GenerationTaskEntity task) {
        if (task.getUserId() == null || task.getReservedCredits() <= 0) {
            return;
        }
        GenerationCreditStatus creditStatus = GenerationCreditStatus.valueOf(task.getCreditStatus());
        if (creditStatus == GenerationCreditStatus.RESERVED) {
            userAccountService.releaseReservedCredits(task.getUserId(), task.getReservedCredits());
            task.setCreditStatus(GenerationCreditStatus.RELEASED.name());
        }
    }

    private void refundCredits(GenerationTaskEntity task) {
        if (task.getUserId() == null || task.getReservedCredits() <= 0) {
            return;
        }
        GenerationCreditStatus creditStatus = GenerationCreditStatus.valueOf(task.getCreditStatus());
        if (creditStatus == GenerationCreditStatus.CONSUMED) {
            userAccountService.refundConsumedCredits(task.getUserId(), task.getReservedCredits());
            task.setCreditStatus(GenerationCreditStatus.RELEASED.name());
            return;
        }
        if (creditStatus == GenerationCreditStatus.RESERVED) {
            userAccountService.releaseReservedCredits(task.getUserId(), task.getReservedCredits());
            task.setCreditStatus(GenerationCreditStatus.RELEASED.name());
        }
    }

    private void requireProviderTaskId(GenerationTaskEntity task) {
        if (task.getProviderTaskId() == null || task.getProviderTaskId().isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "providerTaskId is required for RUNNING");
        }
    }

    private int normalizeProgress(Integer progressPercent, int minValue, int maxValue) {
        if (progressPercent == null) {
            return minValue;
        }
        return Math.max(minValue, Math.min(maxValue, progressPercent));
    }

    private String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private void validateTransition(GenerationStatus currentStatus, GenerationStatus targetStatus) {
        if (currentStatus == targetStatus && targetStatus == GenerationStatus.RUNNING) {
            return;
        }

        EnumSet<GenerationStatus> allowedTargets = switch (currentStatus) {
            case CREATED -> EnumSet.of(GenerationStatus.AUDITING, GenerationStatus.READY_TO_DISPATCH, GenerationStatus.FAILED);
            case AUDITING -> EnumSet.of(GenerationStatus.READY_TO_DISPATCH, GenerationStatus.FAILED);
            case READY_TO_DISPATCH -> EnumSet.of(GenerationStatus.RUNNING, GenerationStatus.FAILED);
            case RUNNING -> EnumSet.of(GenerationStatus.RUNNING, GenerationStatus.POST_PROCESSING, GenerationStatus.FAILED);
            case POST_PROCESSING -> EnumSet.of(GenerationStatus.SUCCESS, GenerationStatus.FAILED);
            case SUCCESS -> EnumSet.of(GenerationStatus.REFUNDED);
            case FAILED -> EnumSet.of(GenerationStatus.REFUNDED);
            case REFUNDED -> EnumSet.noneOf(GenerationStatus.class);
        };

        if (!allowedTargets.contains(targetStatus)) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    HttpStatus.CONFLICT,
                    "Invalid generation status transition: " + currentStatus + " -> " + targetStatus
            );
        }
    }
}
