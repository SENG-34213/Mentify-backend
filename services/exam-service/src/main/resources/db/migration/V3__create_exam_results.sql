CREATE TABLE exam_results (
    id UUID NOT NULL,
    exam_id UUID NOT NULL,
    student_id UUID NOT NULL,
    marks_obtained NUMERIC(8, 2),
    attendance_status VARCHAR(20),
    result_status VARCHAR(20) NOT NULL,
    grade VARCHAR(10),
    remarks VARCHAR(1000),
    marked_by UUID,
    marked_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_exam_results PRIMARY KEY (id),
    CONSTRAINT fk_exam_results_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT uq_exam_results_exam_student UNIQUE (exam_id, student_id),
    CONSTRAINT chk_exam_results_marks_non_negative CHECK (marks_obtained IS NULL OR marks_obtained >= 0),
    CONSTRAINT chk_exam_results_attendance_status
        CHECK (attendance_status IS NULL OR attendance_status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT chk_exam_results_result_status CHECK (result_status IN ('NOT_MARKED', 'PASS', 'FAIL'))
);

CREATE INDEX idx_exam_results_exam_id ON exam_results (exam_id);
CREATE INDEX idx_exam_results_student_id ON exam_results (student_id);
