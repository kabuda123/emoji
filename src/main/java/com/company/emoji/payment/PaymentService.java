package com.company.emoji.payment;

import com.company.emoji.audit.AuditEventService;
import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.payment.dto.CreditLedgerEntryResponse;
import com.company.emoji.payment.dto.CreditLedgerQueryResponse;
import com.company.emoji.payment.dto.VerifyIapRequest;
import com.company.emoji.payment.dto.VerifyIapResponse;
import com.company.emoji.payment.entity.IapOrderEntity;
import com.company.emoji.user.CreditLedgerRepository;
import com.company.emoji.user.UserAccountService;
import com.company.emoji.user.entity.CreditLedgerEntryEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private static final String ORDER_STATUS_VERIFIED = "VERIFIED";

    private final UserAccountService userAccountService;
    private final IapOrderRepository iapOrderRepository;
    private final CreditLedgerRepository creditLedgerRepository;
    private final AuditEventService auditEventService;

    public PaymentService(
            UserAccountService userAccountService,
            IapOrderRepository iapOrderRepository,
            CreditLedgerRepository creditLedgerRepository,
            AuditEventService auditEventService
    ) {
        this.userAccountService = userAccountService;
        this.iapOrderRepository = iapOrderRepository;
        this.creditLedgerRepository = creditLedgerRepository;
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

    @Transactional(readOnly = true)
    public CreditLedgerQueryResponse getLedgerForUser(String userId) {
        userAccountService.requireActiveUser(userId);
        List<CreditLedgerEntryResponse> entries = creditLedgerRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .sorted(Comparator.comparing(CreditLedgerEntryEntity::getCreatedAt).reversed())
                .map(this::toLedgerEntry)
                .toList();
        return new CreditLedgerQueryResponse(entries, entries.size());
    }

    @Transactional(readOnly = true)
    public CreditLedgerQueryResponse getLedgerInternal(String userId, String generationTaskId, String iapOrderId) {
        if (isBlank(userId) && isBlank(generationTaskId) && isBlank(iapOrderId)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "At least one filter is required");
        }

        List<CreditLedgerEntryEntity> candidates = !isBlank(iapOrderId)
                ? creditLedgerRepository.findAllByIapOrderIdOrderByCreatedAtAsc(iapOrderId)
                : !isBlank(generationTaskId)
                ? creditLedgerRepository.findAllByGenerationTaskIdOrderByCreatedAtAsc(generationTaskId)
                : creditLedgerRepository.findAllByUserIdOrderByCreatedAtAsc(userId);

        List<CreditLedgerEntryResponse> entries = candidates.stream()
                .filter(entry -> isBlank(userId) || userId.equals(entry.getUserId()))
                .filter(entry -> isBlank(generationTaskId) || generationTaskId.equals(entry.getGenerationTaskId()))
                .filter(entry -> isBlank(iapOrderId) || iapOrderId.equals(entry.getIapOrderId()))
                .sorted(Comparator.comparing(CreditLedgerEntryEntity::getCreatedAt).reversed())
                .map(this::toLedgerEntry)
                .toList();
        return new CreditLedgerQueryResponse(entries, entries.size());
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

        IapOrderEntity order = new IapOrderEntity();
        order.setId("iap_" + UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setProductId(request.productId());
        order.setTransactionId(request.transactionId());
        order.setReceiptData(request.receiptData());
        order.setStatus(ORDER_STATUS_VERIFIED);
        order.setCreditsGranted(creditsGranted);
        order.setBalanceAfter(0);
        order.setVerifiedAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        iapOrderRepository.save(order);
        int balanceAfter = userAccountService.grantCredits(userId, creditsGranted, order.getId());
        order.setBalanceAfter(balanceAfter);
        order.setUpdatedAt(Instant.now());
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

    private CreditLedgerEntryResponse toLedgerEntry(CreditLedgerEntryEntity entry) {
        return new CreditLedgerEntryResponse(
                entry.getId(),
                entry.getEntryType(),
                entry.getAvailableDelta(),
                entry.getFrozenDelta(),
                entry.getBalanceAfterAvailable(),
                entry.getBalanceAfterFrozen(),
                entry.getGenerationTaskId(),
                entry.getIapOrderId(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
