CREATE TABLE IF NOT EXISTS teacher_guidance_notes (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    note VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_teacher_guidance_notes_student ON teacher_guidance_notes(student_id, created_at DESC);
