CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(64) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE IF NOT EXISTS businesses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    business_type VARCHAR(64) NOT NULL,
    country_code VARCHAR(3) NOT NULL DEFAULT 'KE',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Africa/Nairobi'
);

CREATE TABLE IF NOT EXISTS business_memberships (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_business_memberships_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_business_memberships_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_business_membership UNIQUE (business_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_business_memberships_business_id ON business_memberships(business_id);
CREATE INDEX IF NOT EXISTS idx_business_memberships_user_id ON business_memberships(user_id);
