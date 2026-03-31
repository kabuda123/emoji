package com.company.emoji.audit;

import com.company.emoji.audit.entity.AuditEventEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditEventService {
    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String eventType, String actorType, String generationTaskId, String providerTaskId, String payload) {
        AuditEventEntity event = new AuditEventEntity();
        event.setId("audit_" + UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(eventType);
        event.setActorType(actorType);
        event.setGenerationTaskId(generationTaskId);
        event.setProviderTaskId(providerTaskId);
        event.setPayload(payload);
        event.setCreatedAt(Instant.now());
        auditEventRepository.save(event);
    }

    @Transactional
    public void recordCleanup(String eventType, String actorType, String userId, String cleanupJobId, String payload) {
        AuditEventEntity event = new AuditEventEntity();
        event.setId("audit_" + UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(eventType);
        event.setActorType(actorType);
        event.setUserId(userId);
        event.setCleanupJobId(cleanupJobId);
        event.setPayload(payload);
        event.setCreatedAt(Instant.now());
        auditEventRepository.save(event);
    }
}
