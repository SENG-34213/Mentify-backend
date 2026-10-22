CREATE TABLE IF NOT EXISTS attendance_sessions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    attendance_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_attendance_session_course_date_start
        UNIQUE (course_id, attendance_date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_attendance_session_course_date
    ON attendance_sessions (course_id, attendance_date);

CREATE TABLE IF NOT EXISTS attendance_records (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(500),
    marked_by UUID,
    marked_at TIMESTAMP,
    CONSTRAINT uk_attendance_record_session_student
        UNIQUE (attendance_session_id, student_id),
    CONSTRAINT fk_attendance_record_session
        FOREIGN KEY (attendance_session_id)
        REFERENCES attendance_sessions (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_attendance_record_session_student
    ON attendance_records (attendance_session_id, student_id);

CREATE INDEX IF NOT EXISTS idx_attendance_record_student
    ON attendance_records (student_id);

CREATE TABLE IF NOT EXISTS calendar_projection_attempts (
    id UUID PRIMARY KEY,
    source_type VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    state VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL,
    last_error VARCHAR(1000),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_calendar_projection_source
        UNIQUE (source_type, source_id)
);
