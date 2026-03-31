package com.company.emoji;

import com.company.emoji.auth.JwtTokenService;
import com.company.emoji.user.UserRepository;
import com.company.emoji.user.entity.AppUserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmojiApplicationTests {
    private static final String CURRENT_USER_ID = "usr_test_123";
    private static final String CURRENT_USER_EMAIL = "current.user@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void bootstrapShouldReturnEnvelope() throws Exception {
        mockMvc.perform(get("/api/config/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productName").value("Original Style Emoji Tool"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void templateListShouldReturnSeededPlaceholders() throws Exception {
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("comic"))
                .andExpect(jsonPath("$.data[1].id").value("sticker"));
    }

    @Test
    void createGenerationShouldReturnAcceptedEnvelope() throws Exception {
        mockMvc.perform(post("/api/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idem-demo-1")
                        .content("""
                                {
                                  \"templateId\": \"comic\",
                                  \"inputObjectKey\": \"uploads/demo/input.png\",
                                  \"count\": 2
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.pollAfterSeconds").value(5));
    }

    @Test
    void invalidEmailShouldReturnValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/email/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"email\": \"bad-email\",
                                  \"scene\": \"LOGIN\"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.email").exists());
    }

    @Test
    void emailLoginShouldIssueJwtInTestProfile() throws Exception {
        mockMvc.perform(post("/api/auth/email/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@example.com",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());

        assertThat(userRepository.findByProviderAndExternalSubject("EMAIL", "demo@example.com")).isPresent();
    }

    @Test
    void historyShouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void historyShouldAcceptValidBearerToken() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");

        mockMvc.perform(get("/api/history")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskId").value("task_" + CURRENT_USER_ID + "_1"));
    }

    @Test
    void creditsBalanceShouldRejectMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/credits/balance")
                        .header("Authorization", "Bearer broken-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void creditsBalanceShouldUsePersistedUserAccount() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 321, 12, "ACTIVE");

        mockMvc.perform(get("/api/credits/balance")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableCredits").value(321))
                .andExpect(jsonPath("$.data.frozenCredits").value(12))
                .andExpect(jsonPath("$.data.currency").value("CREDITS"));
    }

    @Test
    void deleteAccountShouldRequireValidBearerToken() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");

        mockMvc.perform(post("/api/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken())
                        .content("""
                                {
                                  "reason": "no longer needed",
                                  "confirmText": "DELETE"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        AppUserEntity user = userRepository.findById(CURRENT_USER_ID).orElseThrow();
        assertThat(user.getStatus()).isEqualTo("DELETION_REQUESTED");
        assertThat(user.getDeletionScheduledAt()).isNotNull();
    }

    @Test
    void deleteHistoryShouldReturnUserScopedIdentifier() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");

        mockMvc.perform(delete("/api/history/task_demo_1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.historyId").value("task_demo_1@" + CURRENT_USER_ID));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenService.issueAccessToken(CURRENT_USER_ID, Map.of("provider", "EMAIL", "email", CURRENT_USER_EMAIL));
    }

    private void persistUser(String userId, String provider, String email, int availableCredits, int frozenCredits, String status) {
        Instant now = Instant.now();
        AppUserEntity user = new AppUserEntity();
        user.setId(userId);
        user.setProvider(provider);
        user.setExternalSubject(email);
        user.setEmail(email);
        user.setStatus(status);
        user.setAvailableCredits(availableCredits);
        user.setFrozenCredits(frozenCredits);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }
}
