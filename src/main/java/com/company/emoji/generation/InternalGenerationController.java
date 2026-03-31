package com.company.emoji.generation;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.common.security.InternalApiGuard;
import com.company.emoji.generation.dto.GenerationDetailResponse;
import com.company.emoji.generation.dto.InternalGenerationStatusUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/generations")
public class InternalGenerationController {
    private final GenerationService generationService;
    private final GenerationDispatchService generationDispatchService;
    private final InternalApiGuard internalApiGuard;

    public InternalGenerationController(
            GenerationService generationService,
            GenerationDispatchService generationDispatchService,
            InternalApiGuard internalApiGuard
    ) {
        this.generationService = generationService;
        this.generationDispatchService = generationDispatchService;
        this.internalApiGuard = internalApiGuard;
    }

    @PostMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<GenerationDetailResponse>> updateGenerationStatus(
            @PathVariable String taskId,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody InternalGenerationStatusUpdateRequest request
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(generationService.updateStatus(taskId, request), TraceIdContext.currentTraceId()));
    }

    @PostMapping("/{taskId}/dispatch")
    public ResponseEntity<ApiResponse<GenerationDetailResponse>> dispatchGeneration(
            @PathVariable String taskId,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(generationDispatchService.dispatch(taskId), TraceIdContext.currentTraceId()));
    }
}
