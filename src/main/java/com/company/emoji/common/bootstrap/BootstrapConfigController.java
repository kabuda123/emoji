package com.company.emoji.common.bootstrap;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class BootstrapConfigController {
    private final BootstrapConfigService bootstrapConfigService;

    public BootstrapConfigController(BootstrapConfigService bootstrapConfigService) {
        this.bootstrapConfigService = bootstrapConfigService;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiResponse<BootstrapConfigResponse>> bootstrap() {
        return ResponseEntity.ok(ApiResponse.ok(bootstrapConfigService.load(), TraceIdContext.currentTraceId()));
    }
}