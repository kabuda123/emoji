package com.company.emoji.generation;

import com.company.emoji.auth.AuthenticatedUser;
import com.company.emoji.auth.CurrentUserContext;
import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.generation.dto.CreateGenerationRequest;
import com.company.emoji.generation.dto.CreateGenerationResponse;
import com.company.emoji.generation.dto.DeleteHistoryResponse;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.HistoryItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GenerationController {
    private final GenerationService generationService;
    private final CurrentUserContext currentUserContext;

    public GenerationController(GenerationService generationService, CurrentUserContext currentUserContext) {
        this.generationService = generationService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping("/generations")
    public ResponseEntity<ApiResponse<CreateGenerationResponse>> createGeneration(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateGenerationRequest request) {
        String currentUserId = currentUserContext.getCurrentUser()
                .map(AuthenticatedUser::userId)
                .orElse(null);
        return ResponseEntity.accepted().body(ApiResponse.ok(generationService.create(currentUserId, request, idempotencyKey), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/generations/{taskId}")
    public ResponseEntity<ApiResponse<GenerationDetailResponse>> getGeneration(@PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.ok(generationService.getDetail(taskId), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<HistoryItemResponse>>> listHistory() {
        AuthenticatedUser currentUser = currentUserContext.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(generationService.listHistory(currentUser.userId()), TraceIdContext.currentTraceId()));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<DeleteHistoryResponse>> deleteHistory(@PathVariable("id") String historyId) {
        AuthenticatedUser currentUser = currentUserContext.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(generationService.deleteHistory(currentUser.userId(), historyId), TraceIdContext.currentTraceId()));
    }
}
