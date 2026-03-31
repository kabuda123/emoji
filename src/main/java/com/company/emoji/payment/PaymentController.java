package com.company.emoji.payment;

import com.company.emoji.common.api.ApiResponse;
import com.company.emoji.common.api.TraceIdContext;
import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.payment.dto.VerifyIapRequest;
import com.company.emoji.payment.dto.VerifyIapResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/iap/verify")
    public ResponseEntity<ApiResponse<VerifyIapResponse>> verifyIap(@Valid @RequestBody VerifyIapRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.verify(request), TraceIdContext.currentTraceId()));
    }

    @GetMapping("/credits/balance")
    public ResponseEntity<ApiResponse<CreditBalanceResponse>> getCreditsBalance() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getBalance(), TraceIdContext.currentTraceId()));
    }
}