DO $$
BEGIN
    -- Older databases may have created this column as VARCHAR even though
    -- Course.gradeId is a UUID and the original schema expects UUID.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'courses'
          AND column_name = 'grade_id'
          AND data_type = 'character varying'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM courses
            WHERE grade_id IS NULL
               OR btrim(grade_id) !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
        ) THEN
            RAISE EXCEPTION
                'Cannot convert courses.grade_id to UUID: one or more values are not valid UUIDs';
        END IF;

        ALTER TABLE courses
            ALTER COLUMN grade_id TYPE UUID
            USING btrim(grade_id)::uuid;
    END IF;
END $$;
