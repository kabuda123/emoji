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

    public UserAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        return new DeleteAccountResponse("SCHEDULED", scheduledDeletionAt);
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
