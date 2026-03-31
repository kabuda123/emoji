package com.company.emoji.admin;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.common.bootstrap.BootstrapConfigAdminUpdateRequest;
import com.company.emoji.common.bootstrap.BootstrapConfigResponse;
import com.company.emoji.common.bootstrap.BootstrapConfigService;
import com.company.emoji.common.security.InternalApiGuard;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/admin/config")
public class AdminBootstrapConfigController {
    private final BootstrapConfigService bootstrapConfigService;
    private final InternalApiGuard internalApiGuard;

    public AdminBootstrapConfigController(BootstrapConfigService bootstrapConfigService, InternalApiGuard internalApiGuard) {
        this.bootstrapConfigService = bootstrapConfigService;
        this.internalApiGuard = internalApiGuard;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiResponse<BootstrapConfigResponse>> getBootstrap(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(bootstrapConfigService.load(), TraceIdContext.currentTraceId()));
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<ApiResponse<BootstrapConfigResponse>> updateBootstrap(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody BootstrapConfigAdminUpdateRequest request
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(bootstrapConfigService.update(request), TraceIdContext.currentTraceId()));
    }
}
