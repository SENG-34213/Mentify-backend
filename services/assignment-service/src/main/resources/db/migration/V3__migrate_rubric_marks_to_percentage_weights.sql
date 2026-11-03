ALTER TABLE assignment_rubric_items
    ADD COLUMN IF NOT EXISTS percentage_weight NUMERIC(5, 2);

WITH rubric_totals AS (
    SELECT assignment_id, SUM(marks)::NUMERIC AS total_marks
    FROM assignment_rubric_items
    GROUP BY assignment_id
)
UPDATE assignment_rubric_items rubric
SET percentage_weight = CASE
    WHEN totals.total_marks > 0
        THEN ROUND((rubric.marks::NUMERIC * 100) / totals.total_marks, 2)
    ELSE 0.00
END
FROM rubric_totals totals
WHERE rubric.assignment_id = totals.assignment_id
  AND rubric.percentage_weight IS NULL;

ALTER TABLE assignment_rubric_items
    ALTER COLUMN percentage_weight SET DEFAULT 0.00,
    ALTER COLUMN percentage_weight SET NOT NULL;
