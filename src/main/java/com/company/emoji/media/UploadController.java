package com.company.emoji.media;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.media.dto.UploadPolicyRequest;
import com.company.emoji.media.dto.UploadPolicyResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private final UploadPolicyService uploadPolicyService;

    public UploadController(UploadPolicyService uploadPolicyService) {
        this.uploadPolicyService = uploadPolicyService;
    }

    @PostMapping("/policy")
    public ResponseEntity<ApiResponse<UploadPolicyResponse>> createPolicy(@Valid @RequestBody UploadPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(uploadPolicyService.createPolicy(request), TraceIdContext.currentTraceId()));
    }
}