package com.company.emoji.generation;

import com.company.emoji.generation.entity.GenerationTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenerationTaskRepository extends JpaRepository<GenerationTaskEntity, String> {
    Optional<GenerationTaskEntity> findByIdAndDeletedFalse(String id);
    Optional<GenerationTaskEntity> findFirstByUserIdAndIdempotencyKeyAndDeletedFalse(String userId, String idempotencyKey);
    List<GenerationTaskEntity> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);
    List<GenerationTaskEntity> findAllByUserIdOrderByCreatedAtAsc(String userId);
    Optional<GenerationTaskEntity> findByIdAndUserIdAndDeletedFalse(String id, String userId);
    Optional<GenerationTaskEntity> findByProviderTaskIdAndDeletedFalse(String providerTaskId);
}
