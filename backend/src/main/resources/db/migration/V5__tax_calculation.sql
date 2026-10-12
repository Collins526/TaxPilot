-- Progressive bands used by PAYE and income tax schedules (versioned via rule_code + rule_version).
CREATE TABLE IF NOT EXISTS tax_rule_bands (
    id UUID PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    rule_version VARCHAR(64) NOT NULL,
    band_order INTEGER NOT NULL,
    lower_bound DECIMAL(19,2) NOT NULL,
    upper_bound DECIMAL(19,2),
    rate DECIMAL(9,6) NOT NULL,
    CONSTRAINT uq_tax_rule_band UNIQUE (rule_code, rule_version, band_order),
    CONSTRAINT ck_tax_rule_band_order CHECK (band_order >= 1),
    CONSTRAINT ck_tax_rule_band_rate CHECK (rate >= 0 AND rate <= 1)
);

CREATE INDEX IF NOT EXISTS idx_tax_rule_bands_schedule
    ON tax_rule_bands (rule_code, rule_version, band_order);

-- Flat rates / reliefs on the existing schedule rules.
UPDATE tax_rule_versions SET rate = 0.160000, threshold = 5000000.00 WHERE rule_code = 'VAT';
UPDATE tax_rule_versions SET rate = 0.030000, threshold = 5000000.00 WHERE rule_code = 'TURNOVER_TAX';
UPDATE tax_rule_versions SET rate = 0.300000, threshold = NULL WHERE rule_code = 'CORPORATION_TAX';
UPDATE tax_rule_versions SET rate = 0.050000, threshold = NULL WHERE rule_code = 'WITHHOLDING_TAX';
UPDATE tax_rule_versions SET rate = NULL, threshold = 2400.00 WHERE rule_code = 'PAYE';
UPDATE tax_rule_versions SET rate = NULL, threshold = 28800.00 WHERE rule_code = 'INCOME_TAX';

-- PAYE monthly bands (KE-CALENDAR-2026.1).
INSERT INTO tax_rule_bands (id, rule_code, rule_version, band_order, lower_bound, upper_bound, rate) VALUES
    ('00000000-0000-0000-0000-000000000101', 'PAYE', 'KE-CALENDAR-2026.1', 1, 0.00, 24000.00, 0.100000),
    ('00000000-0000-0000-0000-000000000102', 'PAYE', 'KE-CALENDAR-2026.1', 2, 24000.00, 32333.00, 0.250000),
    ('00000000-0000-0000-0000-000000000103', 'PAYE', 'KE-CALENDAR-2026.1', 3, 32333.00, 500000.00, 0.300000),
    ('00000000-0000-0000-0000-000000000104', 'PAYE', 'KE-CALENDAR-2026.1', 4, 500000.00, 800000.00, 0.325000),
    ('00000000-0000-0000-0000-000000000105', 'PAYE', 'KE-CALENDAR-2026.1', 5, 800000.00, NULL, 0.350000);

-- Income tax annual bands (KE-CALENDAR-2026.1).
INSERT INTO tax_rule_bands (id, rule_code, rule_version, band_order, lower_bound, upper_bound, rate) VALUES
    ('00000000-0000-0000-0000-000000000201', 'INCOME_TAX', 'KE-CALENDAR-2026.1', 1, 0.00, 288000.00, 0.100000),
    ('00000000-0000-0000-0000-000000000202', 'INCOME_TAX', 'KE-CALENDAR-2026.1', 2, 288000.00, 388000.00, 0.250000),
    ('00000000-0000-0000-0000-000000000203', 'INCOME_TAX', 'KE-CALENDAR-2026.1', 3, 388000.00, 6000000.00, 0.300000),
    ('00000000-0000-0000-0000-000000000204', 'INCOME_TAX', 'KE-CALENDAR-2026.1', 4, 6000000.00, 9600000.00, 0.325000),
    ('00000000-0000-0000-0000-000000000205', 'INCOME_TAX', 'KE-CALENDAR-2026.1', 5, 9600000.00, NULL, 0.350000);

-- Filing-period inputs supplied by the business.
CREATE TABLE IF NOT EXISTS filing_periods (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    taxable_sales DECIMAL(19,2),
    input_vat DECIMAL(19,2),
    payroll_gross DECIMAL(19,2),
    withholding_base DECIMAL(19,2),
    taxable_profit DECIMAL(19,2),
    etims_declared_sales DECIMAL(19,2),
    notes VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    CONSTRAINT fk_filing_periods_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT uq_filing_period UNIQUE (business_id, period_start, period_end),
    CONSTRAINT ck_filing_period_range CHECK (period_end >= period_start)
);

CREATE INDEX IF NOT EXISTS idx_filing_periods_business ON filing_periods (business_id, period_end DESC);

-- Immutable record of each computation, including the rule version used.
CREATE TABLE IF NOT EXISTS tax_calculations (
    id UUID PRIMARY KEY,
    filing_period_id UUID NOT NULL,
    business_id UUID NOT NULL,
    tax_type VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_version VARCHAR(64) NOT NULL,
    rate DECIMAL(9,6),
    amount DECIMAL(19,2) NOT NULL,
    basis VARCHAR(512),
    computed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tax_calculations_period FOREIGN KEY (filing_period_id) REFERENCES filing_periods(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tax_calculations_period ON tax_calculations (filing_period_id);
CREATE INDEX IF NOT EXISTS idx_tax_calculations_business ON tax_calculations (business_id, computed_at DESC);
