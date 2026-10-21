ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_code VARCHAR(50);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS level VARCHAR(100);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS semester VARCHAR(100);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS max_capacity INTEGER;

ALTER TABLE courses ADD CONSTRAINT chk_courses_max_capacity_non_negative
    CHECK (max_capacity IS NULL OR max_capacity >= 0);
