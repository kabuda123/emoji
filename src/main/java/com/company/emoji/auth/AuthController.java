package com.company.emoji.auth;

import com.company.emoji.auth.dto.AppleLoginRequest;
import com.company.emoji.auth.dto.AuthSessionResponse;
import com.company.emoji.auth.dto.EmailLoginRequest;
import com.company.emoji.auth.dto.EmailSendCodeRequest;
import com.company.emoji.auth.dto.EmailSendCodeResponse;
import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/apple/login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithApple(request), TraceIdContext.currentTraceId()));
    }

    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<EmailSendCodeResponse>> sendEmailCode(@Valid @RequestBody EmailSendCodeRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.ok(authService.sendEmailCode(request), TraceIdContext.currentTraceId()));
    }

    @PostMapping("/email/login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> emailLogin(@Valid @RequestBody EmailLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithEmail(request), TraceIdContext.currentTraceId()));
    }
}