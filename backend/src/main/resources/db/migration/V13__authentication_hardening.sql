CREATE TABLE login_attempts (
    username VARCHAR(120) PRIMARY KEY,
    failed_count INTEGER NOT NULL DEFAULT 0,
    first_failed_at TIMESTAMPTZ,
    locked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX audit_logs_action_created_idx ON audit_logs (action, created_at DESC);
