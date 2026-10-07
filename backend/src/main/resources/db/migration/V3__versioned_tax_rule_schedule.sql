CREATE TABLE tax_rule_versions (
    id UUID PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    rule_version VARCHAR(64) NOT NULL,
    frequency VARCHAR(32) NOT NULL,
    due_day INTEGER,
    effective_from DATE NOT NULL,
    effective_to DATE,
    rate DECIMAL(19,6),
    threshold DECIMAL(19,2),
    source VARCHAR(512) NOT NULL,
    CONSTRAINT uq_tax_rule_code_version UNIQUE (rule_code, rule_version),
    CONSTRAINT ck_tax_rule_due_day CHECK (due_day IS NULL OR (due_day >= 1 AND due_day <= 31))
);

CREATE INDEX idx_tax_rule_versions_effective
    ON tax_rule_versions (rule_code, effective_from, effective_to);

INSERT INTO tax_rule_versions (
    id, rule_code, rule_version, frequency, due_day, effective_from, effective_to, rate, threshold, source
) VALUES
    ('00000000-0000-0000-0000-000000000001', 'PAYE', 'KE-CALENDAR-2026.1', 'MONTHLY', 9, '2026-01-01', NULL, NULL, NULL, 'Kenya Revenue Authority PAYE guidance'),
    ('00000000-0000-0000-0000-000000000002', 'VAT', 'KE-CALENDAR-2026.1', 'MONTHLY', 20, '2026-01-01', NULL, NULL, NULL, 'Kenya Revenue Authority VAT guidance'),
    ('00000000-0000-0000-0000-000000000003', 'WITHHOLDING_TAX', 'KE-CALENDAR-2026.1', 'EVENT_BASED', NULL, '2026-01-01', NULL, NULL, NULL, 'Kenya Revenue Authority withholding tax guidance'),
    ('00000000-0000-0000-0000-000000000004', 'INCOME_TAX', 'KE-CALENDAR-2026.1', 'ANNUAL', NULL, '2026-01-01', NULL, NULL, NULL, 'Kenya Revenue Authority income tax guidance'),
    ('00000000-0000-0000-0000-000000000005', 'CORPORATION_TAX', 'KE-CALENDAR-2026.1', 'ANNUAL', NULL, '2026-01-01', NULL, NULL, NULL, 'Kenya Revenue Authority corporation tax guidance'),
    ('00000000-0000-0000-0000-000000000006', 'TURNOVER_TAX', 'KE-CALENDAR-2026.1', 'MONTHLY', 20, '2026-01-01', NULL, NULL, NULL, 'Kenya Revenue Authority turnover tax guidance');
