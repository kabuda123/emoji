package com.company.emoji.generation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "generation_task")
public class GenerationTaskEntity {
    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Column(name = "input_object_key", nullable = false)
    private String inputObjectKey;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(nullable = false)
    private String status;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "preview_urls", nullable = false)
    private String previewUrls;

    @Column(name = "result_urls", nullable = false)
    private String resultUrls;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "provider_task_id")
    private String providerTaskId;

    @Column(name = "reserved_credits", nullable = false)
    private int reservedCredits;

    @Column(name = "credit_status", nullable = false)
    private String creditStatus;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getInputObjectKey() { return inputObjectKey; }
    public void setInputObjectKey(String inputObjectKey) { this.inputObjectKey = inputObjectKey; }
    public int getRequestedCount() { return requestedCount; }
    public void setRequestedCount(int requestedCount) { this.requestedCount = requestedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getPreviewUrls() { return previewUrls; }
    public void setPreviewUrls(String previewUrls) { this.previewUrls = previewUrls; }
    public String getResultUrls() { return resultUrls; }
    public void setResultUrls(String resultUrls) { this.resultUrls = resultUrls; }
    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getProviderTaskId() { return providerTaskId; }
    public void setProviderTaskId(String providerTaskId) { this.providerTaskId = providerTaskId; }
    public int getReservedCredits() { return reservedCredits; }
    public void setReservedCredits(int reservedCredits) { this.reservedCredits = reservedCredits; }
    public String getCreditStatus() { return creditStatus; }
    public void setCreditStatus(String creditStatus) { this.creditStatus = creditStatus; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
