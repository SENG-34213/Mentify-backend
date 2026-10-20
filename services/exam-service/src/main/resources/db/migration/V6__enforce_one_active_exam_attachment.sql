WITH ranked_attachments AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY exam_id ORDER BY created_at DESC, id DESC) AS row_number
    FROM exam_attachments
    WHERE active = TRUE
)
UPDATE exam_attachments
SET active = FALSE
WHERE id IN (SELECT id FROM ranked_attachments WHERE row_number > 1);

-- Keep the lookup efficient across PostgreSQL and the H2 test database.
-- The service deactivates existing attachments when saving their replacement.
CREATE INDEX IF NOT EXISTS idx_exam_attachments_active_exam
    ON exam_attachments (exam_id, active);
