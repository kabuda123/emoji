package com.company.emoji.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUserEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "external_subject", nullable = false)
    private String externalSubject;

    @Column
    private String email;

    @Column(nullable = false)
    private String status;

    @Column(name = "available_credits", nullable = false)
    private int availableCredits;

    @Column(name = "frozen_credits", nullable = false)
    private int frozenCredits;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getExternalSubject() { return externalSubject; }
    public void setExternalSubject(String externalSubject) { this.externalSubject = externalSubject; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAvailableCredits() { return availableCredits; }
    public void setAvailableCredits(int availableCredits) { this.availableCredits = availableCredits; }
    public int getFrozenCredits() { return frozenCredits; }
    public void setFrozenCredits(int frozenCredits) { this.frozenCredits = frozenCredits; }
    public Instant getDeletionRequestedAt() { return deletionRequestedAt; }
    public void setDeletionRequestedAt(Instant deletionRequestedAt) { this.deletionRequestedAt = deletionRequestedAt; }
    public Instant getDeletionScheduledAt() { return deletionScheduledAt; }
    public void setDeletionScheduledAt(Instant deletionScheduledAt) { this.deletionScheduledAt = deletionScheduledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
