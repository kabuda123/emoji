package com.company.emoji.user;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.payment.dto.CreditBalanceResponse;
import com.company.emoji.user.dto.DeleteAccountRequest;
import com.company.emoji.user.dto.DeleteAccountResponse;
import com.company.emoji.user.entity.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserAccountService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETION_REQUESTED = "DELETION_REQUESTED";
    private static final int INITIAL_AVAILABLE_CREDITS = 240;

    private final UserRepository userRepository;
    private final AccountCleanupService accountCleanupService;

    public UserAccountService(UserRepository userRepository, AccountCleanupService accountCleanupService) {
        this.userRepository = userRepository;
        this.accountCleanupService = accountCleanupService;
    }

    @Transactional
    public UserLoginResult findOrCreateEmailUser(String normalizedEmail) {
        return findOrCreateUser("EMAIL", normalizedEmail, normalizedEmail);
    }

    @Transactional
    public UserLoginResult findOrCreateAppleUser(String externalSubject) {
        return findOrCreateUser("APPLE", externalSubject, null);
    }

    @Transactional(readOnly = true)
    public AppUserEntity requireActiveUser(String userId) {
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "User not found"));
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "User is not active");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public CreditBalanceResponse getCreditBalance(String userId) {
        AppUserEntity user = requireActiveUser(userId);
        return new CreditBalanceResponse(user.getAvailableCredits(), user.getFrozenCredits(), "CREDITS");
    }

    @Transactional
    public DeleteAccountResponse requestDeletion(String userId, DeleteAccountRequest request) {
        AppUserEntity user = requireActiveUser(userId);
        Instant now = Instant.now();
        Instant scheduledDeletionAt = now.plus(7, ChronoUnit.DAYS);
        user.setStatus(STATUS_DELETION_REQUESTED);
        user.setDeletionRequestedAt(now);
        user.setDeletionScheduledAt(scheduledDeletionAt);
        user.setUpdatedAt(now);
        return new DeleteAccountResponse("SCHEDULED", scheduledDeletionAt, accountCleanupService.scheduleDeletion(user, request).getId());
    }

    @Transactional
    public void reserveCredits(String userId, int credits) {
        if (credits <= 0) {
            return;
        }
        AppUserEntity user = requireActiveUser(userId);
        if (user.getAvailableCredits() < credits) {
            throw new ApiException(ApiErrorCode.INSUFFICIENT_CREDITS, HttpStatus.CONFLICT, "Insufficient available credits");
        }
        user.setAvailableCredits(user.getAvailableCredits() - credits);
        user.setFrozenCredits(user.getFrozenCredits() + credits);
        user.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void consumeReservedCredits(String userId, int credits) {
        if (credits <= 0) {
            return;
        }
        AppUserEntity user = requireActiveUser(userId);
        if (user.getFrozenCredits() < credits) {
            throw new ApiException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT, "Reserved credits are inconsistent");
        }
        user.setFrozenCredits(user.getFrozenCredits() - credits);
        user.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void releaseReservedCredits(String userId, int credits) {
        if (credits <= 0) {
            return;
        }
        AppUserEntity user = requireActiveUser(userId);
        if (user.getFrozenCredits() < credits) {
            throw new ApiException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT, "Reserved credits are inconsistent");
        }
        user.setFrozenCredits(user.getFrozenCredits() - credits);
        user.setAvailableCredits(user.getAvailableCredits() + credits);
        user.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void refundConsumedCredits(String userId, int credits) {
        if (credits <= 0) {
            return;
        }
        AppUserEntity user = requireActiveUser(userId);
        user.setAvailableCredits(user.getAvailableCredits() + credits);
        user.setUpdatedAt(Instant.now());
    }

    @Transactional
    public int grantCredits(String userId, int credits) {
        if (credits <= 0) {
            return requireActiveUser(userId).getAvailableCredits();
        }
        AppUserEntity user = requireActiveUser(userId);
        user.setAvailableCredits(user.getAvailableCredits() + credits);
        user.setUpdatedAt(Instant.now());
        return user.getAvailableCredits();
    }

    private UserLoginResult findOrCreateUser(String provider, String externalSubject, String email) {
        return userRepository.findByProviderAndExternalSubject(provider, externalSubject)
                .map(user -> {
                    if (email != null && !email.equals(user.getEmail())) {
                        user.setEmail(email);
                        user.setUpdatedAt(Instant.now());
                    }
                    return new UserLoginResult(user.getId(), false);
                })
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    AppUserEntity user = new AppUserEntity();
                    user.setId("usr_" + UUID.randomUUID().toString().replace("-", ""));
                    user.setProvider(provider);
                    user.setExternalSubject(externalSubject);
                    user.setEmail(email);
                    user.setStatus(STATUS_ACTIVE);
                    user.setAvailableCredits(INITIAL_AVAILABLE_CREDITS);
                    user.setFrozenCredits(0);
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                    userRepository.save(user);
                    return new UserLoginResult(user.getId(), true);
                });
    }
}
