package com.company.emoji.media;

import com.company.emoji.media.domain.MediaAssetRole;
import com.company.emoji.media.domain.MediaAssetStatus;
import com.company.emoji.media.entity.MediaAssetEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MediaMetadataService {
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetService mediaAssetService;

    public MediaMetadataService(MediaAssetRepository mediaAssetRepository, MediaAssetService mediaAssetService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional
    public void recordSourcePolicy(String objectKey, String contentType) {
        MediaAssetEntity asset = mediaAssetRepository.findByObjectKey(objectKey).orElseGet(MediaAssetEntity::new);
        Instant now = Instant.now();
        if (asset.getId() == null) {
            asset.setId("media_" + UUID.randomUUID().toString().replace("-", ""));
            asset.setCreatedAt(now);
        }
        asset.setObjectKey(objectKey);
        asset.setAssetRole(MediaAssetRole.SOURCE.name());
        asset.setContentType(contentType);
        asset.setPublicUrl(mediaAssetService.toPublicUrl(objectKey));
        asset.setSourceStatus(MediaAssetStatus.POLICY_ISSUED.name());
        asset.setUpdatedAt(now);
        mediaAssetRepository.save(asset);
    }

    @Transactional
    public void attachSourceToTask(String ownerUserId, String generationTaskId, String objectKey) {
        MediaAssetEntity asset = mediaAssetRepository.findByObjectKey(objectKey).orElseGet(MediaAssetEntity::new);
        Instant now = Instant.now();
        if (asset.getId() == null) {
            asset.setId("media_" + UUID.randomUUID().toString().replace("-", ""));
            asset.setCreatedAt(now);
            asset.setObjectKey(objectKey);
            asset.setAssetRole(MediaAssetRole.SOURCE.name());
            asset.setPublicUrl(mediaAssetService.toPublicUrl(objectKey));
        }
        asset.setOwnerUserId(ownerUserId);
        asset.setGenerationTaskId(generationTaskId);
        asset.setSourceStatus(MediaAssetStatus.ATTACHED.name());
        asset.setUpdatedAt(now);
        mediaAssetRepository.save(asset);
    }

    @Transactional
    public void recordGeneratedAssets(String generationTaskId, String ownerUserId, String providerTaskId, List<String> previewKeys, List<String> resultKeys) {
        recordAssets(generationTaskId, ownerUserId, providerTaskId, previewKeys, MediaAssetRole.PREVIEW);
        recordAssets(generationTaskId, ownerUserId, providerTaskId, resultKeys, MediaAssetRole.RESULT);
    }

    private void recordAssets(String generationTaskId, String ownerUserId, String providerTaskId, List<String> objectKeys, MediaAssetRole role) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (String objectKey : objectKeys) {
            if (objectKey == null || objectKey.isBlank()) {
                continue;
            }
            MediaAssetEntity asset = mediaAssetRepository.findByObjectKey(objectKey).orElseGet(MediaAssetEntity::new);
            if (asset.getId() == null) {
                asset.setId("media_" + UUID.randomUUID().toString().replace("-", ""));
                asset.setCreatedAt(now);
            }
            asset.setObjectKey(objectKey);
            asset.setAssetRole(role.name());
            asset.setOwnerUserId(ownerUserId);
            asset.setGenerationTaskId(generationTaskId);
            asset.setProviderTaskId(providerTaskId);
            asset.setPublicUrl(mediaAssetService.toPublicUrl(objectKey));
            asset.setSourceStatus(MediaAssetStatus.GENERATED.name());
            asset.setUpdatedAt(now);
            mediaAssetRepository.save(asset);
        }
    }
}
