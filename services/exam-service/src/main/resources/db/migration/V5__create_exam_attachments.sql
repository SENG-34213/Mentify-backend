CREATE TABLE exam_attachments (
    id UUID NOT NULL,
    exam_id UUID NOT NULL,
    material_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by UUID NOT NULL,
    student_visible BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_exam_attachments PRIMARY KEY (id),
    CONSTRAINT fk_exam_attachments_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT uk_exam_attachments_material UNIQUE (material_id)
);

CREATE INDEX idx_exam_attachments_exam_id ON exam_attachments (exam_id);
