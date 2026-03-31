package com.company.emoji;

import com.company.emoji.auth.JwtTokenService;
import com.company.emoji.generation.GenerationTaskRepository;
import com.company.emoji.generation.entity.GenerationTaskEntity;
import com.company.emoji.user.UserRepository;
import com.company.emoji.user.entity.AppUserEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

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
    private static final String INTERNAL_API_TOKEN = "test-internal-token";
    private static final String PROVIDER_WEBHOOK_TOKEN = "test-provider-webhook-token";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

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
        MvcResult result = mockMvc.perform(post("/api/generations")
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
                .andExpect(jsonPath("$.data.pollAfterSeconds").value(5))
                .andReturn();

        String taskId = responseValue(result, "/data/taskId");
        assertThat(generationTaskRepository.findByIdAndDeletedFalse(taskId)).isPresent();
    }

    @Test
    void getGenerationShouldReadPersistedTask() throws Exception {
        GenerationTaskEntity task = persistGenerationTask("task_detail_1", null, "comic", "uploads/demo/input.png", "RUNNING", 42, "https://example.com/previews/task_detail_1-1.png", "", false, "provider-detail-1");

        mockMvc.perform(get("/api/generations/" + task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(task.getId()))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.progressPercent").value(42))
                .andExpect(jsonPath("$.data.previewUrls[0]").value("https://example.com/previews/task_detail_1-1.png"));
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
        persistGenerationTask("task_hist_1", CURRENT_USER_ID, "comic", "uploads/demo/1.png", "SUCCESS", 100, "https://example.com/previews/task_hist_1.png", "https://example.com/results/task_hist_1.png", false, "provider-hist-1");
        persistGenerationTask("task_hist_2", CURRENT_USER_ID, "sticker", "uploads/demo/2.png", "RUNNING", 45, "https://example.com/previews/task_hist_2.png", "", false, "provider-hist-2");

        mockMvc.perform(get("/api/history")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskId").value("task_hist_2"))
                .andExpect(jsonPath("$.data[1].taskId").value("task_hist_1"));
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
        persistGenerationTask("task_demo_1", CURRENT_USER_ID, "comic", "uploads/demo/1.png", "SUCCESS", 100, "https://example.com/previews/task_demo_1.png", "https://example.com/results/task_demo_1.png", false, "provider-demo-1");

        mockMvc.perform(delete("/api/history/task_demo_1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.historyId").value("task_demo_1"));

        assertThat(generationTaskRepository.findByIdAndUserIdAndDeletedFalse("task_demo_1", CURRENT_USER_ID)).isEmpty();
    }

    @Test
    void authenticatedCreateShouldAppearInHistory() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");

        String taskId = createAuthenticatedTask("comic", "uploads/demo/auth-input.png");

        mockMvc.perform(get("/api/history")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskId").value(taskId));

        AppUserEntity user = userRepository.findById(CURRENT_USER_ID).orElseThrow();
        assertThat(user.getAvailableCredits()).isEqualTo(228);
        assertThat(user.getFrozenCredits()).isEqualTo(12);
    }

    @Test
    void authenticatedCreateShouldRejectInsufficientCredits() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 4, 0, "ACTIVE");

        mockMvc.perform(post("/api/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken())
                        .content("""
                                {
                                  "templateId": "comic",
                                  "inputObjectKey": "uploads/demo/no-balance.png",
                                  "count": 2
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_CREDITS"));
    }

    @Test
    void internalStatusUpdateShouldRequireInternalToken() throws Exception {
        persistGenerationTask("task_internal_1", null, "comic", "uploads/demo/internal.png", "CREATED", 0, "https://example.com/previews/task_internal_1.png", "", false, null);

        mockMvc.perform(post("/api/internal/generations/task_internal_1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "AUDITING"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void internalStatusUpdateShouldAdvanceLifecycleToSuccess() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");
        String taskId = createAuthenticatedTask("comic", "uploads/demo/internal2.png");

        postInternalStatus(taskId, """
                {
                  "status": "AUDITING"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUDITING"));

        postInternalStatus(taskId, """
                {
                  "status": "READY_TO_DISPATCH"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_TO_DISPATCH"));

        postInternalStatus(taskId, """
                {
                  "status": "RUNNING",
                  "providerTaskId": "provider-task-2",
                  "progressPercent": 46,
                  "previewUrls": ["https://example.com/previews/task_internal_2-a.png"]
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.progressPercent").value(46));

        postInternalStatus(taskId, """
                {
                  "status": "POST_PROCESSING",
                  "progressPercent": 95
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POST_PROCESSING"));

        postInternalStatus(taskId, """
                {
                  "status": "SUCCESS",
                  "resultUrls": ["https://example.com/results/task_internal_2-1.png"]
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.resultUrls[0]").value("https://example.com/results/task_internal_2-1.png"));

        mockMvc.perform(get("/api/generations/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.progressPercent").value(100))
                .andExpect(jsonPath("$.data.resultUrls[0]").value("https://example.com/results/task_internal_2-1.png"));

        AppUserEntity user = userRepository.findById(CURRENT_USER_ID).orElseThrow();
        assertThat(user.getAvailableCredits()).isEqualTo(228);
        assertThat(user.getFrozenCredits()).isEqualTo(0);
    }

    @Test
    void internalStatusUpdateShouldRejectInvalidTransition() throws Exception {
        persistGenerationTask("task_internal_3", null, "comic", "uploads/demo/internal3.png", "CREATED", 0, "https://example.com/previews/task_internal_3.png", "", false, null);

        postInternalStatus("task_internal_3", """
                {
                  "status": "SUCCESS",
                  "resultUrls": ["https://example.com/results/task_internal_3.png"]
                }
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void internalStatusUpdateShouldRecordFailureReason() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");
        String taskId = createAuthenticatedTask("comic", "uploads/demo/internal4.png");

        postInternalStatus(taskId, """
                {
                  "status": "FAILED",
                  "failedReason": "provider timeout"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failedReason").value("provider timeout"));

        AppUserEntity user = userRepository.findById(CURRENT_USER_ID).orElseThrow();
        assertThat(user.getAvailableCredits()).isEqualTo(240);
        assertThat(user.getFrozenCredits()).isEqualTo(0);
    }

    @Test
    void refundedStatusShouldReturnConsumedCredits() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");
        String taskId = createAuthenticatedTask("comic", "uploads/demo/internal5.png");

        postInternalStatus(taskId, """
                {
                  "status": "AUDITING"
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "READY_TO_DISPATCH"
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "RUNNING",
                  "providerTaskId": "provider-task-5",
                  "progressPercent": 40
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "POST_PROCESSING"
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "SUCCESS",
                  "resultUrls": ["https://example.com/results/task_internal_5.png"]
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "REFUNDED"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        AppUserEntity user = userRepository.findById(CURRENT_USER_ID).orElseThrow();
        assertThat(user.getAvailableCredits()).isEqualTo(240);
        assertThat(user.getFrozenCredits()).isEqualTo(0);
    }

    @Test
    void dispatchShouldRequireReadyToDispatchStatus() throws Exception {
        persistGenerationTask("task_dispatch_1", null, "comic", "uploads/demo/dispatch1.png", "CREATED", 0, "", "", false, null);

        mockMvc.perform(post("/api/internal/generations/task_dispatch_1/dispatch")
                        .header("X-Internal-Token", INTERNAL_API_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void dispatchShouldAdvanceTaskToRunning() throws Exception {
        persistGenerationTask("task_dispatch_2", null, "comic", "uploads/demo/dispatch2.png", "READY_TO_DISPATCH", 10, "", "", false, null);

        mockMvc.perform(post("/api/internal/generations/task_dispatch_2/dispatch")
                        .header("X-Internal-Token", INTERNAL_API_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.previewUrls[0]").value("https://provider.example.com/mock/previews/mock_task_dispatch_2.png"));

        GenerationTaskEntity task = generationTaskRepository.findByIdAndDeletedFalse("task_dispatch_2").orElseThrow();
        assertThat(task.getProviderTaskId()).isEqualTo("mock_task_dispatch_2");
    }

    @Test
    void providerWebhookShouldRequireProviderToken() throws Exception {
        persistGenerationTask("task_webhook_1", null, "comic", "uploads/demo/webhook1.png", "RUNNING", 45, "", "", false, "mock_task_webhook_1");

        mockMvc.perform(post("/api/providers/mock/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerTaskId": "mock_task_webhook_1",
                                  "status": "SUCCESS",
                                  "resultUrls": ["https://provider.example.com/mock/results/mock_task_webhook_1.png"]
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void providerWebhookShouldDriveTaskToSuccess() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");
        String taskId = createAuthenticatedTask("comic", "uploads/demo/webhook-success.png");

        postInternalStatus(taskId, """
                {
                  "status": "AUDITING"
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "READY_TO_DISPATCH"
                }
                """).andExpect(status().isOk());

        mockMvc.perform(post("/api/internal/generations/" + taskId + "/dispatch")
                        .header("X-Internal-Token", INTERNAL_API_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));

        mockMvc.perform(post("/api/providers/mock/webhook")
                        .header("X-Provider-Token", PROVIDER_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerTaskId": "mock_%s",
                                  "status": "POST_PROCESSING",
                                  "progressPercent": 94
                                }
                                """.formatted(taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POST_PROCESSING"));

        mockMvc.perform(post("/api/providers/mock/webhook")
                        .header("X-Provider-Token", PROVIDER_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerTaskId": "mock_%s",
                                  "status": "SUCCESS",
                                  "resultUrls": ["https://provider.example.com/mock/results/%s.png"]
                                }
                                """.formatted(taskId, taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.resultUrls[0]").value("https://provider.example.com/mock/results/" + taskId + ".png"));
    }

    @Test
    void providerWebhookShouldDriveTaskToFailed() throws Exception {
        persistUser(CURRENT_USER_ID, "EMAIL", CURRENT_USER_EMAIL, 240, 0, "ACTIVE");
        String taskId = createAuthenticatedTask("comic", "uploads/demo/webhook-failed.png");

        postInternalStatus(taskId, """
                {
                  "status": "AUDITING"
                }
                """).andExpect(status().isOk());

        postInternalStatus(taskId, """
                {
                  "status": "READY_TO_DISPATCH"
                }
                """).andExpect(status().isOk());

        mockMvc.perform(post("/api/internal/generations/" + taskId + "/dispatch")
                        .header("X-Internal-Token", INTERNAL_API_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/providers/mock/webhook")
                        .header("X-Provider-Token", PROVIDER_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerTaskId": "mock_%s",
                                  "status": "FAILED",
                                  "failedReason": "provider mock failure"
                                }
                                """.formatted(taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failedReason").value("provider mock failure"));
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

    private GenerationTaskEntity persistGenerationTask(
            String taskId,
            String userId,
            String templateId,
            String inputObjectKey,
            String status,
            int progressPercent,
            String previewUrls,
            String resultUrls,
            boolean deleted,
            String providerTaskId
    ) {
        Instant now = Instant.now();
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setId(taskId);
        task.setUserId(userId);
        task.setTemplateId(templateId);
        task.setInputObjectKey(inputObjectKey);
        task.setRequestedCount(2);
        task.setStatus(status);
        task.setProgressPercent(progressPercent);
        task.setPreviewUrls(previewUrls);
        task.setResultUrls(resultUrls);
        task.setProviderTaskId(providerTaskId);
        task.setReservedCredits(0);
        task.setCreditStatus("NONE");
        task.setDeleted(deleted);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return generationTaskRepository.save(task);
    }

    private ResultActions postInternalStatus(String taskId, String content) throws Exception {
        return mockMvc.perform(post("/api/internal/generations/" + taskId + "/status")
                .header("X-Internal-Token", INTERNAL_API_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));
    }

    private String responseValue(MvcResult result, String pointer) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return jsonNode.at(pointer).asText();
    }

    private String createAuthenticatedTask(String templateId, String inputObjectKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken())
                        .content("""
                                {
                                  "templateId": "%s",
                                  "inputObjectKey": "%s",
                                  "count": 2
                                }
                                """.formatted(templateId, inputObjectKey)))
                .andExpect(status().isAccepted())
                .andReturn();
        return responseValue(result, "/data/taskId");
    }
}
