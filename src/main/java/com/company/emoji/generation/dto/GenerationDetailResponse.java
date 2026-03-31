package com.company.emoji.generation.dto;

import com.company.emoji.generation.domain.GenerationStatus;

import java.util.List;

public record GenerationDetailResponse(
        String taskId,
        GenerationStatus status,
        int progressPercent,
        List<String> previewUrls,
        List<String> resultUrls,
        String failedReason
) {
}