CREATE TABLE IF NOT EXISTS filing_deadline_statuses (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    tax_type VARCHAR(64) NOT NULL,
    due_date DATE NOT NULL,
    filing_status VARCHAR(24) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_filing_deadline_status_business
        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT uk_filing_deadline_status_business_tax_date
        UNIQUE (business_id, tax_type, due_date)
);

CREATE INDEX IF NOT EXISTS idx_filing_deadline_status_business
    ON filing_deadline_statuses (business_id, due_date);
