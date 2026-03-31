package com.company.emoji.admin;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.common.security.InternalApiGuard;
import com.company.emoji.template.TemplateService;
import com.company.emoji.template.dto.InternalTemplateUpdateRequest;
import com.company.emoji.template.dto.TemplateDetailResponse;
import com.company.emoji.template.dto.TemplateSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/admin/templates")
public class AdminTemplateController {
    private final TemplateService templateService;
    private final InternalApiGuard internalApiGuard;

    public AdminTemplateController(TemplateService templateService, InternalApiGuard internalApiGuard) {
        this.templateService = templateService;
        this.internalApiGuard = internalApiGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateSummaryResponse>>> listTemplates(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(templateService.listTemplates(), TraceIdContext.currentTraceId()));
    }

    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDetailResponse>> updateTemplate(
            @PathVariable String id,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody InternalTemplateUpdateRequest request
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(templateService.updateTemplate(id, request), TraceIdContext.currentTraceId()));
    }
}
