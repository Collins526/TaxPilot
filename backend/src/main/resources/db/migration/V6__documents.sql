CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    size_bytes BIGINT NOT NULL,
    category VARCHAR(64),
    storage_provider VARCHAR(32) NOT NULL,
    storage_url VARCHAR(1024) NOT NULL,
    storage_public_id VARCHAR(512) NOT NULL,
    uploaded_by UUID,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documents_business ON documents (business_id, uploaded_at DESC);
