package com.company.emoji.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "media_asset")
public class MediaAssetEntity {
    @Id
    private String id;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "asset_role", nullable = false)
    private String assetRole;

    @Column(name = "owner_user_id")
    private String ownerUserId;

    @Column(name = "generation_task_id")
    private String generationTaskId;

    @Column(name = "provider_task_id")
    private String providerTaskId;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "public_url", nullable = false)
    private String publicUrl;

    @Column(name = "source_status", nullable = false)
    private String sourceStatus;

    @Column(name = "lifecycle_status", nullable = false)
    private String lifecycleStatus;

    @Column(name = "purge_scheduled_at")
    private Instant purgeScheduledAt;

    @Column(name = "purged_at")
    private Instant purgedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getAssetRole() { return assetRole; }
    public void setAssetRole(String assetRole) { this.assetRole = assetRole; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getGenerationTaskId() { return generationTaskId; }
    public void setGenerationTaskId(String generationTaskId) { this.generationTaskId = generationTaskId; }
    public String getProviderTaskId() { return providerTaskId; }
    public void setProviderTaskId(String providerTaskId) { this.providerTaskId = providerTaskId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public Instant getPurgeScheduledAt() { return purgeScheduledAt; }
    public void setPurgeScheduledAt(Instant purgeScheduledAt) { this.purgeScheduledAt = purgeScheduledAt; }
    public Instant getPurgedAt() { return purgedAt; }
    public void setPurgedAt(Instant purgedAt) { this.purgedAt = purgedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
