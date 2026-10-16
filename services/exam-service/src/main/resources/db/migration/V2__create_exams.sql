CREATE TABLE exams (
    id UUID NOT NULL,
    course_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    total_marks NUMERIC(8, 2) NOT NULL,
    pass_marks NUMERIC(8, 2) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_exams PRIMARY KEY (id),
    CONSTRAINT chk_exams_title_non_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT chk_exams_time_range CHECK (start_time < end_time),
    CONSTRAINT chk_exams_total_marks_positive CHECK (total_marks > 0),
    CONSTRAINT chk_exams_pass_marks_range CHECK (pass_marks >= 0 AND pass_marks <= total_marks),
    CONSTRAINT chk_exams_type CHECK (type IN ('MIDTERM', 'FINAL', 'PRACTICAL', 'OTHER')),
    CONSTRAINT chk_exams_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'MARKING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_exams_course_id ON exams (course_id);
CREATE INDEX idx_exams_exam_date ON exams (exam_date);
