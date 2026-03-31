package com.company.emoji.generation;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.generation.domain.GenerationStatus;
import com.company.emoji.generation.dto.CreateGenerationRequest;
import com.company.emoji.generation.dto.CreateGenerationResponse;
import com.company.emoji.generation.dto.DeleteHistoryResponse;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.HistoryItemResponse;
import com.company.emoji.user.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationService {
    private final UserAccountService userAccountService;

    public GenerationService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public CreateGenerationResponse create(CreateGenerationRequest request, String idempotencyKey) {
        return new CreateGenerationResponse(
                "task_" + UUID.randomUUID().toString().replace("-", ""),
                GenerationStatus.CREATED,
                5
        );
    }

    public GenerationDetailResponse getDetail(String taskId) {
        if (taskId.isBlank()) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Generation task not found");
        }
        return new GenerationDetailResponse(
                taskId,
                GenerationStatus.RUNNING,
                60,
                List.of("https://example.com/previews/" + taskId + "/1.png"),
                List.of(),
                null
        );
    }

    public List<HistoryItemResponse> listHistory(String userId) {
        userAccountService.requireActiveUser(userId);
        return List.of(
                new HistoryItemResponse("task_" + userId + "_1", "Comic Style", GenerationStatus.SUCCESS, "https://example.com/history/task_demo_1-cover.png", Instant.now()),
                new HistoryItemResponse("task_" + userId + "_2", "Sticker Style", GenerationStatus.RUNNING, "https://example.com/history/task_demo_2-cover.png", Instant.now().minusSeconds(600))
        );
    }

    public DeleteHistoryResponse deleteHistory(String userId, String historyId) {
        userAccountService.requireActiveUser(userId);
        return new DeleteHistoryResponse(true, historyId + "@" + userId);
    }
}
