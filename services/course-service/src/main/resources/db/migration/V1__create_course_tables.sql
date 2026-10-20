CREATE TABLE IF NOT EXISTS grades (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    name VARCHAR(100) NOT NULL, description TEXT, monthly_fee NUMERIC(12,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL, CONSTRAINT uk_grades_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS grade_monthly_fees (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    grade_id UUID NOT NULL UNIQUE REFERENCES grades(id), monthly_fee NUMERIC(12,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_name VARCHAR(150) NOT NULL, course_description TEXT, course_thumbnail VARCHAR(255),
    status VARCHAR(20) NOT NULL, subject VARCHAR(150), is_online BOOLEAN NOT NULL,
    is_visible BOOLEAN NOT NULL, assigned_teacher_id UUID NOT NULL, grade_id UUID NOT NULL,
    is_published BOOLEAN NOT NULL, published_date DATE, number_of_students INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS modules (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    title VARCHAR(150) NOT NULL, description TEXT, module_image VARCHAR(255), sequence_order INTEGER NOT NULL,
    is_visible BOOLEAN NOT NULL, date_duration INTEGER, course_id UUID NOT NULL REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS lessons (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    title VARCHAR(150) NOT NULL, description TEXT, duration_minutes INTEGER NOT NULL DEFAULT 1,
    content_markdown TEXT, is_visible BOOLEAN NOT NULL, release_date DATE,
    module_id UUID NOT NULL REFERENCES modules(id)
);

CREATE TABLE IF NOT EXISTS learning_materials (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    title VARCHAR(150) NOT NULL, type VARCHAR(20) NOT NULL, file_url VARCHAR(500),
    original_filename VARCHAR(255), content_type VARCHAR(255), file_size BIGINT, status VARCHAR(30),
    material_id UUID, module_id UUID NOT NULL REFERENCES modules(id), lesson_id UUID REFERENCES lessons(id)
);

CREATE TABLE IF NOT EXISTS course_announcements (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_id UUID NOT NULL, author_id UUID NOT NULL, title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL, published_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS student_lesson_progress (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    student_id UUID NOT NULL, course_id UUID NOT NULL, lesson_id UUID NOT NULL,
    completed BOOLEAN NOT NULL, last_accessed_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_student_lesson_progress_student_lesson UNIQUE (student_id, lesson_id)
);

CREATE INDEX IF NOT EXISTS idx_course_announcements_course_published
    ON course_announcements(course_id, published_at);
