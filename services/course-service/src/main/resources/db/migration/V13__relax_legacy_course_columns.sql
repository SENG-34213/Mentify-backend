-- These columns belong to the legacy course schema and are no longer mapped
-- by com.mentify.entity.Course. Keep existing values, but do not require
-- new rows to populate fields that the current API does not use.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'courses'
          AND column_name = 'course_fee_monthly'
    ) THEN
        ALTER TABLE courses ALTER COLUMN course_fee_monthly DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'courses'
          AND column_name = 'teacher_id'
    ) THEN
        ALTER TABLE courses ALTER COLUMN teacher_id DROP NOT NULL;
    END IF;
END $$;
