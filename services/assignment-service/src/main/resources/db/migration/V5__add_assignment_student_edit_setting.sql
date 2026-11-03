ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS allow_student_edits BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE assignments
SET allow_student_edits = TRUE
WHERE allow_student_edits IS NULL;
