DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'keycloak_user_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'auth_user_id'
    ) THEN
        ALTER TABLE users RENAME COLUMN keycloak_user_id TO auth_user_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'uk_users_keycloak_user_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'uk_users_auth_user_id'
    ) THEN
        ALTER INDEX uk_users_keycloak_user_id RENAME TO uk_users_auth_user_id;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_auth_user_id ON users (auth_user_id);
