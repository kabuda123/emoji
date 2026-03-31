package com.company.emoji.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity {
    @Id
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "generation_task_id")
    private String generationTaskId;

    @Column(name = "provider_task_id")
    private String providerTaskId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getGenerationTaskId() { return generationTaskId; }
    public void setGenerationTaskId(String generationTaskId) { this.generationTaskId = generationTaskId; }
    public String getProviderTaskId() { return providerTaskId; }
    public void setProviderTaskId(String providerTaskId) { this.providerTaskId = providerTaskId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
