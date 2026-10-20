CREATE TABLE IF NOT EXISTS entrollments (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    student_id UUID NOT NULL, enrolled_on DATE NOT NULL, status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS entrollment_courses (
    entrollment_id UUID NOT NULL REFERENCES entrollments(id) ON DELETE CASCADE,
    course_id UUID NOT NULL,
    PRIMARY KEY (entrollment_id, course_id)
);
