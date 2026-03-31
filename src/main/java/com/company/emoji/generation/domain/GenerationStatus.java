package com.company.emoji.generation.domain;

public enum GenerationStatus {
    CREATED,
    AUDITING,
    READY_TO_DISPATCH,
    RUNNING,
    POST_PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED
}