ALTER TABLE communication_groups
    ADD COLUMN IF NOT EXISTS student_messaging_enabled BOOLEAN NOT NULL DEFAULT TRUE;
