package com.company.emoji.payment;

import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.payment.dto.VerifyIapRequest;
import com.company.emoji.payment.dto.VerifyIapResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public VerifyIapResponse verify(VerifyIapRequest request) {
        return new VerifyIapResponse("iap_order_demo", "VERIFIED", 120, 240);
    }

    public CreditBalanceResponse getBalance(String userId) {
        int personalizedBalance = 240 + Math.floorMod(userId.hashCode(), 50);
        return new CreditBalanceResponse(personalizedBalance, 0, "CREDITS");
    }
}
