CREATE TABLE media_asset (
    id VARCHAR(64) PRIMARY KEY,
    object_key VARCHAR(255) NOT NULL UNIQUE,
    asset_role VARCHAR(32) NOT NULL,
    owner_user_id VARCHAR(64),
    generation_task_id VARCHAR(64),
    provider_task_id VARCHAR(128),
    content_type VARCHAR(128),
    public_url VARCHAR(255) NOT NULL,
    source_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_media_asset_user FOREIGN KEY (owner_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_media_asset_generation_task FOREIGN KEY (generation_task_id) REFERENCES generation_task(id)
);

CREATE TABLE audit_event (
    id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    generation_task_id VARCHAR(64),
    provider_task_id VARCHAR(128),
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_event_generation_task FOREIGN KEY (generation_task_id) REFERENCES generation_task(id)
);
