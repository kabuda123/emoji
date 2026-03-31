package com.company.emoji.common.api;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final ApiErrorCode code;
    private final HttpStatus status;

    public ApiException(ApiErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}