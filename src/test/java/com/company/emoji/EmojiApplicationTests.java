package com.company.emoji;

import com.company.emoji.auth.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

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
        mockMvc.perform(get("/api/history")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskId").value("task_usr_test_123_1"));
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
    void deleteAccountShouldRequireValidBearerToken() throws Exception {
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
    }

    @Test
    void deleteHistoryShouldReturnUserScopedIdentifier() throws Exception {
        mockMvc.perform(delete("/api/history/task_demo_1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.historyId").value("task_demo_1@usr_test_123"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenService.issueAccessToken("usr_test_123", Map.of("provider", "EMAIL", "email", "demo@example.com"));
    }
}
