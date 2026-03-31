package com.company.emoji.user;

import com.company.emoji.audit.AuditEventService;
import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.generation.GenerationTaskRepository;
import com.company.emoji.generation.domain.GenerationLifecycleStatus;
import com.company.emoji.generation.entity.GenerationTaskEntity;
import com.company.emoji.media.MediaAssetRepository;
import com.company.emoji.media.domain.MediaLifecycleStatus;
import com.company.emoji.media.entity.MediaAssetEntity;
import com.company.emoji.user.domain.AccountCleanupJobStatus;
import com.company.emoji.user.dto.AccountCleanupExecutionResponse;
import com.company.emoji.user.dto.DeleteAccountRequest;
import com.company.emoji.user.entity.AccountCleanupJobEntity;
import com.company.emoji.user.entity.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountCleanupService {
    private static final String USER_STATUS_DELETED = "DELETED";

    private final AccountCleanupJobRepository accountCleanupJobRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final UserRepository userRepository;
    private final AuditEventService auditEventService;

    public AccountCleanupService(
            AccountCleanupJobRepository accountCleanupJobRepository,
            GenerationTaskRepository generationTaskRepository,
            MediaAssetRepository mediaAssetRepository,
            UserRepository userRepository,
            AuditEventService auditEventService
    ) {
        this.accountCleanupJobRepository = accountCleanupJobRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.userRepository = userRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public AccountCleanupJobEntity scheduleDeletion(AppUserEntity user, DeleteAccountRequest request) {
        Instant now = Instant.now();
        AccountCleanupJobEntity job = accountCleanupJobRepository.findByUserId(user.getId())
                .orElseGet(AccountCleanupJobEntity::new);
        if (job.getId() == null) {
            job.setId("cleanup_" + UUID.randomUUID().toString().replace("-", ""));
            job.setUserId(user.getId());
            job.setCreatedAt(now);
        }

        job.setStatus(AccountCleanupJobStatus.SCHEDULED.name());
        job.setScheduledAt(user.getDeletionScheduledAt());
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setReason(normalizeReason(request.reason()));
        job.setSummary(null);
        job.setUpdatedAt(now);
        accountCleanupJobRepository.save(job);

        markGenerationTasksForDeletion(user.getId(), user.getDeletionScheduledAt(), now);
        markMediaAssetsForDeletion(user.getId(), user.getDeletionScheduledAt(), now);

        auditEventService.recordCleanup(
                "ACCOUNT_DELETION_REQUESTED",
                "USER",
                user.getId(),
                job.getId(),
                "scheduledAt=" + user.getDeletionScheduledAt()
        );
        return job;
    }

    @Transactional
    public AccountCleanupExecutionResponse executeCleanup(String cleanupJobId) {
        AccountCleanupJobEntity job = accountCleanupJobRepository.findById(cleanupJobId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Cleanup job not found"));
        AppUserEntity user = userRepository.findById(job.getUserId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        if (AccountCleanupJobStatus.COMPLETED.name().equals(job.getStatus())) {
            return buildExecutionResponse(job);
        }

        Instant startedAt = Instant.now();
        job.setStatus(AccountCleanupJobStatus.RUNNING.name());
        job.setStartedAt(startedAt);
        job.setUpdatedAt(startedAt);
        auditEventService.recordCleanup(
                "ACCOUNT_CLEANUP_STARTED",
                "SYSTEM",
                user.getId(),
                job.getId(),
                "scheduledAt=" + job.getScheduledAt()
        );

        List<GenerationTaskEntity> generationTasks = generationTaskRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        List<MediaAssetEntity> mediaAssets = mediaAssetRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(user.getId());
        Instant completedAt = Instant.now();

        for (GenerationTaskEntity task : generationTasks) {
            task.setLifecycleStatus(GenerationLifecycleStatus.PURGED.name());
            task.setPurgeScheduledAt(job.getScheduledAt());
            task.setPurgedAt(completedAt);
            task.setDeleted(true);
            task.setUpdatedAt(completedAt);
        }

        for (MediaAssetEntity asset : mediaAssets) {
            asset.setLifecycleStatus(MediaLifecycleStatus.DELETED.name());
            asset.setPurgeScheduledAt(job.getScheduledAt());
            asset.setPurgedAt(completedAt);
            asset.setUpdatedAt(completedAt);
        }

        user.setStatus(USER_STATUS_DELETED);
        user.setAvailableCredits(0);
        user.setFrozenCredits(0);
        user.setEmail(null);
        user.setExternalSubject("deleted:" + user.getId());
        user.setDeletionCompletedAt(completedAt);
        user.setUpdatedAt(completedAt);

        job.setStatus(AccountCleanupJobStatus.COMPLETED.name());
        job.setCompletedAt(completedAt);
        job.setSummary("generationTasks=" + generationTasks.size() + ";mediaAssets=" + mediaAssets.size());
        job.setUpdatedAt(completedAt);

        auditEventService.recordCleanup(
                "ACCOUNT_CLEANUP_COMPLETED",
                "SYSTEM",
                user.getId(),
                job.getId(),
                job.getSummary()
        );

        return new AccountCleanupExecutionResponse(
                job.getId(),
                user.getId(),
                job.getStatus(),
                generationTasks.size(),
                mediaAssets.size(),
                completedAt
        );
    }

    private void markGenerationTasksForDeletion(String userId, Instant scheduledAt, Instant now) {
        for (GenerationTaskEntity task : generationTaskRepository.findAllByUserIdOrderByCreatedAtAsc(userId)) {
            task.setLifecycleStatus(GenerationLifecycleStatus.DELETION_SCHEDULED.name());
            task.setPurgeScheduledAt(scheduledAt);
            task.setUpdatedAt(now);
        }
    }

    private void markMediaAssetsForDeletion(String userId, Instant scheduledAt, Instant now) {
        for (MediaAssetEntity asset : mediaAssetRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(userId)) {
            asset.setLifecycleStatus(MediaLifecycleStatus.DELETION_SCHEDULED.name());
            asset.setPurgeScheduledAt(scheduledAt);
            asset.setUpdatedAt(now);
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }

    private AccountCleanupExecutionResponse buildExecutionResponse(AccountCleanupJobEntity job) {
        int generationTaskCount = extractCount(job.getSummary(), "generationTasks=");
        int mediaAssetCount = extractCount(job.getSummary(), "mediaAssets=");
        return new AccountCleanupExecutionResponse(
                job.getId(),
                job.getUserId(),
                job.getStatus(),
                generationTaskCount,
                mediaAssetCount,
                job.getCompletedAt()
        );
    }

    private int extractCount(String summary, String prefix) {
        if (summary == null || summary.isBlank()) {
            return 0;
        }
        for (String part : summary.split(";")) {
            if (part.startsWith(prefix)) {
                return Integer.parseInt(part.substring(prefix.length()));
            }
        }
        return 0;
    }
}
