CREATE TABLE app_user (
    id VARCHAR(64) PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    external_subject VARCHAR(128) NOT NULL,
    email VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    available_credits INTEGER NOT NULL,
    frozen_credits INTEGER NOT NULL,
    deletion_requested_at TIMESTAMP,
    deletion_scheduled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_app_user_provider_subject UNIQUE (provider, external_subject),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);
