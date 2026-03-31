CREATE TABLE generation_task (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    template_id VARCHAR(64) NOT NULL,
    input_object_key VARCHAR(255) NOT NULL,
    requested_count INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress_percent INTEGER NOT NULL,
    preview_urls TEXT NOT NULL,
    result_urls TEXT NOT NULL,
    failed_reason VARCHAR(500),
    idempotency_key VARCHAR(128),
    deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_generation_task_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_generation_task_template FOREIGN KEY (template_id) REFERENCES style_template(id)
);
