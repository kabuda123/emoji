package com.company.emoji.auth;

import com.company.emoji.auth.dto.AppleLoginRequest;
import com.company.emoji.auth.dto.AuthSessionResponse;
import com.company.emoji.auth.dto.EmailLoginRequest;
import com.company.emoji.auth.dto.EmailSendCodeRequest;
import com.company.emoji.auth.dto.EmailSendCodeResponse;
import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.common.config.AuthProperties;
import com.company.emoji.user.UserAccountService;
import com.company.emoji.user.UserLoginResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthProperties authProperties;
    private final EmailCodeStore emailCodeStore;
    private final JwtTokenService jwtTokenService;
    private final UserAccountService userAccountService;

    public AuthService(
            AuthProperties authProperties,
            EmailCodeStore emailCodeStore,
            JwtTokenService jwtTokenService,
            UserAccountService userAccountService
    ) {
        this.authProperties = authProperties;
        this.emailCodeStore = emailCodeStore;
        this.jwtTokenService = jwtTokenService;
        this.userAccountService = userAccountService;
    }

    public AuthSessionResponse loginWithApple(AppleLoginRequest request) {
        String externalSubject = hash(request.identityToken());
        UserLoginResult loginResult = userAccountService.findOrCreateAppleUser(externalSubject);
        return issueSession(loginResult.userId(), Map.of("provider", "APPLE"), loginResult.isNewUser());
    }

    public EmailSendCodeResponse sendEmailCode(EmailSendCodeRequest request) {
        String code = authProperties.fixedEmailCodeForDev();
        if (code == null || code.isBlank()) {
            code = randomCode();
        }
        emailCodeStore.put(request.email(), code, authProperties.emailCodeTtlSeconds());
        return new EmailSendCodeResponse(60, maskEmail(request.email()));
    }

    public AuthSessionResponse loginWithEmail(EmailLoginRequest request) {
        boolean fixedCodeMatched = authProperties.fixedEmailCodeForDev() != null
                && !authProperties.fixedEmailCodeForDev().isBlank()
                && authProperties.fixedEmailCodeForDev().equals(request.code());

        if (!fixedCodeMatched && !emailCodeStore.matches(request.email(), request.code())) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Invalid email verification code");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        UserLoginResult loginResult = userAccountService.findOrCreateEmailUser(normalizedEmail);
        Map<String, Object> claims = new HashMap<>();
        claims.put("provider", "EMAIL");
        claims.put("email", normalizedEmail);
        return issueSession(loginResult.userId(), claims, loginResult.isNewUser());
    }

    private AuthSessionResponse issueSession(String userId, Map<String, Object> claims, boolean isNewUser) {
        String accessToken = jwtTokenService.issueAccessToken(userId, claims);
        String refreshToken = jwtTokenService.issueRefreshToken(userId, claims);
        return new AuthSessionResponse(userId, accessToken, refreshToken, authProperties.accessTokenTtlSeconds(), isNewUser);
    }

    private String randomCode() {
        int value = 100000 + (int) (Math.random() * 900000);
        return String.valueOf(value);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex - 1);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte aByte : bytes) {
                builder.append(String.format("%02x", aByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
