package com.company.emoji.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "credit_ledger")
public class CreditLedgerEntryEntity {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "available_delta", nullable = false)
    private int availableDelta;

    @Column(name = "frozen_delta", nullable = false)
    private int frozenDelta;

    @Column(name = "balance_after_available", nullable = false)
    private int balanceAfterAvailable;

    @Column(name = "balance_after_frozen", nullable = false)
    private int balanceAfterFrozen;

    @Column(name = "generation_task_id")
    private String generationTaskId;

    @Column(name = "iap_order_id")
    private String iapOrderId;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public int getAvailableDelta() { return availableDelta; }
    public void setAvailableDelta(int availableDelta) { this.availableDelta = availableDelta; }
    public int getFrozenDelta() { return frozenDelta; }
    public void setFrozenDelta(int frozenDelta) { this.frozenDelta = frozenDelta; }
    public int getBalanceAfterAvailable() { return balanceAfterAvailable; }
    public void setBalanceAfterAvailable(int balanceAfterAvailable) { this.balanceAfterAvailable = balanceAfterAvailable; }
    public int getBalanceAfterFrozen() { return balanceAfterFrozen; }
    public void setBalanceAfterFrozen(int balanceAfterFrozen) { this.balanceAfterFrozen = balanceAfterFrozen; }
    public String getGenerationTaskId() { return generationTaskId; }
    public void setGenerationTaskId(String generationTaskId) { this.generationTaskId = generationTaskId; }
    public String getIapOrderId() { return iapOrderId; }
    public void setIapOrderId(String iapOrderId) { this.iapOrderId = iapOrderId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
