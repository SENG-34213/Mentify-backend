-- Material Service owns uploaded files now. The legacy file_url column is
-- retained for old records, but new rows use material_id instead.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'learning_materials'
          AND column_name = 'file_url'
    ) THEN
        ALTER TABLE learning_materials ALTER COLUMN file_url DROP NOT NULL;
    END IF;
END $$;
