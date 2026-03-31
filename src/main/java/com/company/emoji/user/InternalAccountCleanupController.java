package com.company.emoji.user;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.common.security.InternalApiGuard;
import com.company.emoji.user.dto.AccountCleanupExecutionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/account-cleanup")
public class InternalAccountCleanupController {
    private final AccountCleanupService accountCleanupService;
    private final InternalApiGuard internalApiGuard;

    public InternalAccountCleanupController(
            AccountCleanupService accountCleanupService,
            InternalApiGuard internalApiGuard
    ) {
        this.accountCleanupService = accountCleanupService;
        this.internalApiGuard = internalApiGuard;
    }

    @PostMapping("/{cleanupJobId}/execute")
    public ResponseEntity<ApiResponse<AccountCleanupExecutionResponse>> executeCleanup(
            @PathVariable String cleanupJobId,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(accountCleanupService.executeCleanup(cleanupJobId), TraceIdContext.currentTraceId()));
    }
}
