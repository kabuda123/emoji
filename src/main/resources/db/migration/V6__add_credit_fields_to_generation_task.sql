ALTER TABLE generation_task
ADD COLUMN reserved_credits INTEGER NOT NULL DEFAULT 0;

ALTER TABLE generation_task
ADD COLUMN credit_status VARCHAR(32) NOT NULL DEFAULT 'NONE';
