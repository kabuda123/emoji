package com.company.emoji.user;

import com.company.emoji.auth.AuthenticatedUser;
import com.company.emoji.auth.CurrentUserContext;
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
    private final CurrentUserContext currentUserContext;

    public AccountController(AccountService accountService, CurrentUserContext currentUserContext) {
        this.accountService = accountService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<DeleteAccountResponse>> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        AuthenticatedUser currentUser = currentUserContext.requireCurrentUser();
        return ResponseEntity.accepted().body(ApiResponse.ok(accountService.requestDeletion(currentUser.userId(), request), TraceIdContext.currentTraceId()));
    }
}
