ALTER TABLE auth_users
    ADD COLUMN locked_until TIMESTAMP NULL;

-- Convert legacy permanent locks into the new temporary policy.
UPDATE auth_users
SET locked_until = CURRENT_TIMESTAMP + INTERVAL '15 minutes'
WHERE locked = TRUE
  AND locked_until IS NULL;
