CREATE TABLE iap_order (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    transaction_id VARCHAR(128) NOT NULL UNIQUE,
    receipt_data TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    credits_granted INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    verified_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_iap_order_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
