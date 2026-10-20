CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_id UUID NOT NULL, module_id UUID NOT NULL, lesson_id UUID, teacher_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL, description TEXT, instructions TEXT,
    start_date TIMESTAMP NOT NULL, due_date TIMESTAMP NOT NULL, max_marks INTEGER NOT NULL,
    allowed_attempts INTEGER NOT NULL, allow_late_submission BOOLEAN NOT NULL DEFAULT FALSE,
    late_penalty_percentage INTEGER NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL,
    published_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assignment_attachments (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_id UUID NOT NULL REFERENCES assignments(id), original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255), content_type VARCHAR(255) NOT NULL, file_size BIGINT NOT NULL,
    uploaded_by UUID NOT NULL, material_id UUID
);

CREATE TABLE IF NOT EXISTS assignment_rubric_items (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_id UUID NOT NULL REFERENCES assignments(id), criterion VARCHAR(150) NOT NULL,
    marks INTEGER NOT NULL, display_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_id UUID NOT NULL REFERENCES assignments(id), student_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL, status VARCHAR(20) NOT NULL, content TEXT,
    is_late BOOLEAN NOT NULL DEFAULT FALSE, submitted_at TIMESTAMP, marks NUMERIC(10,2),
    feedback TEXT, graded_at TIMESTAMP, graded_by UUID, returned_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS submission_files (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    submission_id UUID NOT NULL REFERENCES submissions(id), original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255), content_type VARCHAR(255) NOT NULL, file_size BIGINT NOT NULL,
    material_id UUID
);

CREATE TABLE IF NOT EXISTS quizzes (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_id UUID NOT NULL, teacher_id UUID NOT NULL, title VARCHAR(150) NOT NULL,
    description TEXT, duration_minutes INTEGER NOT NULL, total_marks NUMERIC(10,2) NOT NULL DEFAULT 0,
    creation_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL', pass_mark NUMERIC(10,2) NOT NULL,
    start_time TIMESTAMP, end_time TIMESTAMP, max_attempts INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL, show_result_immediately BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    quiz_id UUID NOT NULL REFERENCES quizzes(id), question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL, marks NUMERIC(10,2) NOT NULL, question_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS question_options (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id), option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL, option_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    quiz_id UUID NOT NULL, student_id UUID NOT NULL, attempt_number INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL, submitted_at TIMESTAMP, score NUMERIC(10,2),
    percentage NUMERIC(5,2), status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS student_answers (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id), question_id UUID NOT NULL,
    selected_option_id UUID NOT NULL, correct BOOLEAN, awarded_marks NUMERIC(10,2), answered_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_student_answers_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE TABLE IF NOT EXISTS calendar_projection_attempts (
    id UUID PRIMARY KEY, source_type VARCHAR(20) NOT NULL, source_id UUID NOT NULL,
    state VARCHAR(20) NOT NULL, attempts INTEGER NOT NULL, last_error VARCHAR(1000), updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_calendar_projection_source UNIQUE (source_type, source_id)
);
