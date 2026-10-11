CREATE TABLE exam_calendar_projection_attempts (
    id UUID NOT NULL,
    exam_id UUID NOT NULL,
    state VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_exam_calendar_projection_attempts PRIMARY KEY (id),
    CONSTRAINT uq_exam_calendar_projection_exam UNIQUE (exam_id),
    CONSTRAINT chk_exam_calendar_projection_state CHECK (state IN ('SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_exam_calendar_projection_attempts_non_negative CHECK (attempts >= 0)
);

CREATE INDEX idx_exam_calendar_projection_state
    ON exam_calendar_projection_attempts (state);
