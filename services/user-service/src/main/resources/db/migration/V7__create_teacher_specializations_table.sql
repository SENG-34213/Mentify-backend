CREATE TABLE IF NOT EXISTS teacher_specializations (
    teacher_profile_id UUID NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    CONSTRAINT fk_teacher_specializations_profile
        FOREIGN KEY (teacher_profile_id) REFERENCES teacher_profiles (id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_specializations_profile_id
    ON teacher_specializations (teacher_profile_id);

INSERT INTO teacher_specializations (teacher_profile_id, specialization)
SELECT id, specialization
FROM teacher_profiles
WHERE specialization IS NOT NULL
  AND specialization <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM teacher_specializations
      WHERE teacher_specializations.teacher_profile_id = teacher_profiles.id
        AND teacher_specializations.specialization = teacher_profiles.specialization
  );
