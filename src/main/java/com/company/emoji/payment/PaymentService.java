package com.company.emoji.payment;

import com.company.emoji.audit.AuditEventService;
import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.payment.dto.VerifyIapRequest;
import com.company.emoji.payment.dto.VerifyIapResponse;
import com.company.emoji.payment.entity.IapOrderEntity;
import com.company.emoji.user.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private static final String ORDER_STATUS_VERIFIED = "VERIFIED";

    private final UserAccountService userAccountService;
    private final IapOrderRepository iapOrderRepository;
    private final AuditEventService auditEventService;

    public PaymentService(
            UserAccountService userAccountService,
            IapOrderRepository iapOrderRepository,
            AuditEventService auditEventService
    ) {
        this.userAccountService = userAccountService;
        this.iapOrderRepository = iapOrderRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public VerifyIapResponse verify(String userId, VerifyIapRequest request) {
        userAccountService.requireActiveUser(userId);

        return iapOrderRepository.findByTransactionId(request.transactionId())
                .map(order -> replayOrder(userId, order))
                .orElseGet(() -> createOrder(userId, request));
    }

    public CreditBalanceResponse getBalance(String userId) {
        return userAccountService.getCreditBalance(userId);
    }

    private VerifyIapResponse replayOrder(String userId, IapOrderEntity order) {
        if (!order.getUserId().equals(userId)) {
            throw new ApiException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT, "Transaction already belongs to another user");
        }
        auditEventService.recordUser(
                "IAP_VERIFY_REPLAYED",
                "USER",
                userId,
                "orderId=" + order.getId() + ";transactionId=" + order.getTransactionId()
        );
        return toResponse(order);
    }

    private VerifyIapResponse createOrder(String userId, VerifyIapRequest request) {
        Instant now = Instant.now();
        int creditsGranted = creditsForProduct(request.productId());
        int balanceAfter = userAccountService.grantCredits(userId, creditsGranted);

        IapOrderEntity order = new IapOrderEntity();
        order.setId("iap_" + UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setProductId(request.productId());
        order.setTransactionId(request.transactionId());
        order.setReceiptData(request.receiptData());
        order.setStatus(ORDER_STATUS_VERIFIED);
        order.setCreditsGranted(creditsGranted);
        order.setBalanceAfter(balanceAfter);
        order.setVerifiedAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        iapOrderRepository.save(order);

        auditEventService.recordUser(
                "IAP_VERIFIED",
                "USER",
                userId,
                "orderId=" + order.getId() + ";productId=" + order.getProductId() + ";transactionId=" + order.getTransactionId()
        );
        return toResponse(order);
    }

    private int creditsForProduct(String productId) {
        return switch (productId) {
            case "credits_120" -> 120;
            case "credits_300" -> 300;
            case "credits_680" -> 680;
            default -> throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Unsupported productId");
        };
    }

    private VerifyIapResponse toResponse(IapOrderEntity order) {
        return new VerifyIapResponse(order.getId(), order.getStatus(), order.getCreditsGranted(), order.getBalanceAfter());
    }
}
