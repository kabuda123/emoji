package com.company.emoji.template;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.template.dto.TemplateDetailResponse;
import com.company.emoji.template.dto.TemplateSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateSummaryResponse>>> listTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.listTemplates(), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDetailResponse>> getTemplate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getTemplate(id), TraceIdContext.currentTraceId()));
    }
}