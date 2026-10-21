ALTER TABLE auth_refresh_tokens
    ADD COLUMN remember_me BOOLEAN NOT NULL DEFAULT TRUE;
