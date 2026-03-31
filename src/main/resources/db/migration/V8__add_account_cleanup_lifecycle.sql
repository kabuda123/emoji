ALTER TABLE app_user
    ADD COLUMN deletion_completed_at TIMESTAMP;

ALTER TABLE generation_task
    ADD COLUMN lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE generation_task
    ADD COLUMN purge_scheduled_at TIMESTAMP;

ALTER TABLE generation_task
    ADD COLUMN purged_at TIMESTAMP;

ALTER TABLE media_asset
    ADD COLUMN lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE media_asset
    ADD COLUMN purge_scheduled_at TIMESTAMP;

ALTER TABLE media_asset
    ADD COLUMN purged_at TIMESTAMP;

CREATE TABLE account_cleanup_job (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    reason VARCHAR(255),
    summary VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_cleanup_job_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

ALTER TABLE audit_event
    ADD COLUMN user_id VARCHAR(64);

ALTER TABLE audit_event
    ADD COLUMN cleanup_job_id VARCHAR(64);

ALTER TABLE audit_event
    ADD CONSTRAINT fk_audit_event_user FOREIGN KEY (user_id) REFERENCES app_user(id);

ALTER TABLE audit_event
    ADD CONSTRAINT fk_audit_event_cleanup_job FOREIGN KEY (cleanup_job_id) REFERENCES account_cleanup_job(id);
