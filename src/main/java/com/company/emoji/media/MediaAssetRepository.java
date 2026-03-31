package com.company.emoji.media;

import com.company.emoji.media.entity.MediaAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, String> {
    Optional<MediaAssetEntity> findByObjectKey(String objectKey);
    List<MediaAssetEntity> findAllByGenerationTaskIdOrderByCreatedAtAsc(String generationTaskId);
}
