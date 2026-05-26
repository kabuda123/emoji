CREATE TABLE bootstrap_config_override (
    id VARCHAR(64) PRIMARY KEY,
    product_name VARCHAR(128),
    ios_review_mode BOOLEAN,
    iap_enabled BOOLEAN,
    supported_login_methods VARCHAR(255),
    privacy_url VARCHAR(255),
    terms_url VARCHAR(255),
    ai_auth_url VARCHAR(255),
    generation_min_images INTEGER,
    generation_max_images INTEGER,
    generation_poll_seconds INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
