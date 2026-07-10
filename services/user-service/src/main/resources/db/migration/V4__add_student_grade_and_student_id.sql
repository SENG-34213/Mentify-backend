ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS student_id VARCHAR(20);
ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS grade VARCHAR(2);

CREATE UNIQUE INDEX IF NOT EXISTS uk_student_profiles_student_id ON student_profiles (student_id);
