package com.company.emoji.audit;

import com.company.emoji.audit.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {
    List<AuditEventEntity> findAllByGenerationTaskIdOrderByCreatedAtAsc(String generationTaskId);
    List<AuditEventEntity> findAllByCleanupJobIdOrderByCreatedAtAsc(String cleanupJobId);
}
