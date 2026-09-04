-- Update 1: accounts for the three roles that use the system.
-- Schema only. Demo users are seeded by DemoDataSeeder so that password hashes
-- are produced by the application's own PasswordEncoder instead of being
-- hardcoded into a migration.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Logins are matched case-insensitively, so index the lowered column.
CREATE INDEX idx_users_email_lower ON users (LOWER(email));
