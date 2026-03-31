package com.company.emoji.generation.dto;

public record DeleteHistoryResponse(
        boolean deleted,
        String historyId
) {
}