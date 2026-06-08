CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email VARCHAR(150) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(30),
    account_status VARCHAR(30) NOT NULL DEFAULT 'INVITED',
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    login_attempts INTEGER NOT NULL DEFAULT 0,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login TIMESTAMP,
    keycloak_user_id VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_user_id VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(30) NOT NULL DEFAULT 'INVITED';

ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(150);
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(30);
ALTER TABLE users ALTER COLUMN account_status TYPE VARCHAR(30);
ALTER TABLE users ALTER COLUMN account_status SET DEFAULT 'INVITED';

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_keycloak_user_id ON users (keycloak_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
          AND column_name = 'password'
    ) THEN
        ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
    END IF;
END $$;
