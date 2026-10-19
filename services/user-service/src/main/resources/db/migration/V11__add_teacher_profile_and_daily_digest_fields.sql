ALTER TABLE users
    ADD COLUMN IF NOT EXISTS daily_digest_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS daily_digest_time TIME;

ALTER TABLE teacher_profiles
    ADD COLUMN IF NOT EXISTS academic_title VARCHAR(100);

ALTER TABLE teacher_profiles
    ADD COLUMN IF NOT EXISTS department VARCHAR(150);

ALTER TABLE teacher_profiles
    ADD COLUMN IF NOT EXISTS office_location VARCHAR(150);

ALTER TABLE teacher_profiles
    ADD COLUMN IF NOT EXISTS office_hours VARCHAR(255);

ALTER TABLE teacher_profiles
    ADD COLUMN IF NOT EXISTS biography VARCHAR(2000);

ALTER TABLE teacher_profiles
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
