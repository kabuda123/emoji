package com.company.emoji.payment;

import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.payment.dto.VerifyIapRequest;
import com.company.emoji.payment.dto.VerifyIapResponse;
import com.company.emoji.user.UserAccountService;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final UserAccountService userAccountService;

    public PaymentService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public VerifyIapResponse verify(VerifyIapRequest request) {
        return new VerifyIapResponse("iap_order_demo", "VERIFIED", 120, 240);
    }

    public CreditBalanceResponse getBalance(String userId) {
        return userAccountService.getCreditBalance(userId);
    }
}
