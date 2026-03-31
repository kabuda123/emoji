CREATE TABLE credit_ledger (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    available_delta INTEGER NOT NULL,
    frozen_delta INTEGER NOT NULL,
    balance_after_available INTEGER NOT NULL,
    balance_after_frozen INTEGER NOT NULL,
    generation_task_id VARCHAR(64),
    iap_order_id VARCHAR(64),
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_credit_ledger_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_credit_ledger_generation_task FOREIGN KEY (generation_task_id) REFERENCES generation_task(id),
    CONSTRAINT fk_credit_ledger_iap_order FOREIGN KEY (iap_order_id) REFERENCES iap_order(id)
);
