package com.company.emoji.user;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.user.dto.DeleteAccountRequest;
import com.company.emoji.user.dto.DeleteAccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<DeleteAccountResponse>> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.ok(accountService.requestDeletion(request), TraceIdContext.currentTraceId()));
    }
}