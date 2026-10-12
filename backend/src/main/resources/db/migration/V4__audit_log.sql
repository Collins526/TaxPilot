CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL,
    actor_user_id UUID,
    actor_email VARCHAR(255),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64),
    business_id UUID,
    outcome VARCHAR(16) NOT NULL,
    details VARCHAR(1024)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_business_occurred ON audit_logs (business_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_occurred ON audit_logs (actor_user_id, occurred_at DESC);
