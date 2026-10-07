ALTER TABLE businesses ADD COLUMN IF NOT EXISTS kra_pin VARCHAR(50);
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS industry VARCHAR(255);
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS annual_turnover DECIMAL(19,2) DEFAULT 0;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS employee_count INTEGER DEFAULT 0;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS vat_registered BOOLEAN DEFAULT FALSE;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS etims_registered BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS tax_profiles (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL UNIQUE,
    business_type VARCHAR(64) NOT NULL,
    annual_turnover DECIMAL(19,2),
    monthly_turnover DECIMAL(19,2),
    employee_count INTEGER,
    vat_registered BOOLEAN NOT NULL DEFAULT FALSE,
    vat_registration_date DATE,
    etims_registered BOOLEAN NOT NULL DEFAULT FALSE,
    has_employees BOOLEAN NOT NULL DEFAULT FALSE,
    makes_withholding_payments BOOLEAN NOT NULL DEFAULT FALSE,
    receives_rental_income BOOLEAN NOT NULL DEFAULT FALSE,
    imports_goods BOOLEAN NOT NULL DEFAULT FALSE,
    exports_goods BOOLEAN NOT NULL DEFAULT FALSE,
    has_foreign_transactions BOOLEAN NOT NULL DEFAULT FALSE,
    has_digital_services BOOLEAN NOT NULL DEFAULT FALSE,
    has_excise_activities BOOLEAN NOT NULL DEFAULT FALSE,
    income_tax_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    corporation_tax_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    turnover_tax_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tax_profiles_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);
