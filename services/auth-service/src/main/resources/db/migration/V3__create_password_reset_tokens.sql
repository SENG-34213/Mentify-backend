CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    otp_hash VARCHAR(64) NOT NULL,
    reset_token_hash VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_reset_hash ON password_reset_tokens(reset_token_hash);
