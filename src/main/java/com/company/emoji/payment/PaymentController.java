package com.company.emoji.payment;

import com.company.emoji.auth.AuthenticatedUser;
import com.company.emoji.auth.CurrentUserContext;
import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.common.security.InternalApiGuard;
import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.payment.dto.CreditLedgerQueryResponse;
import com.company.emoji.payment.dto.VerifyIapRequest;
import com.company.emoji.payment.dto.VerifyIapResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService paymentService;
    private final CurrentUserContext currentUserContext;
    private final InternalApiGuard internalApiGuard;

    public PaymentController(
            PaymentService paymentService,
            CurrentUserContext currentUserContext,
            InternalApiGuard internalApiGuard
    ) {
        this.paymentService = paymentService;
        this.currentUserContext = currentUserContext;
        this.internalApiGuard = internalApiGuard;
    }

    @PostMapping("/iap/verify")
    public ResponseEntity<ApiResponse<VerifyIapResponse>> verifyIap(@Valid @RequestBody VerifyIapRequest request) {
        AuthenticatedUser currentUser = currentUserContext.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(paymentService.verify(currentUser.userId(), request), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/credits/balance")
    public ResponseEntity<ApiResponse<CreditBalanceResponse>> getCreditsBalance() {
        AuthenticatedUser currentUser = currentUserContext.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getBalance(currentUser.userId()), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/credits/ledger")
    public ResponseEntity<ApiResponse<CreditLedgerQueryResponse>> getCreditLedger() {
        AuthenticatedUser currentUser = currentUserContext.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getLedgerForUser(currentUser.userId()), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/internal/admin/credits/ledger")
    public ResponseEntity<ApiResponse<CreditLedgerQueryResponse>> getCreditLedgerInternal(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String generationTaskId,
            @RequestParam(required = false) String iapOrderId
    ) {
        internalApiGuard.requireValidToken(internalToken);
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getLedgerInternal(userId, generationTaskId, iapOrderId),
                TraceIdContext.currentTraceId()
        ));
    }
}
