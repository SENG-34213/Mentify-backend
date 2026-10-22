ALTER TABLE courses ADD COLUMN IF NOT EXISTS credits INTEGER;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS schedule VARCHAR(255);

CREATE TABLE IF NOT EXISTS course_grading_weights (
    course_id UUID NOT NULL,
    assessment_type VARCHAR(50) NOT NULL,
    weight NUMERIC(5,2),
    PRIMARY KEY (course_id, assessment_type),
    CONSTRAINT fk_course_grading_weights_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS course_teaching_assistants (
    course_id UUID NOT NULL,
    assistant_id UUID NOT NULL,
    PRIMARY KEY (course_id, assistant_id),
    CONSTRAINT fk_course_teaching_assistants_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);
