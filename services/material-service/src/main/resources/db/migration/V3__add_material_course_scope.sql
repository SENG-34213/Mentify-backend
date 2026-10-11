ALTER TABLE materials
    ADD COLUMN course_id UUID;

CREATE INDEX idx_materials_course_id ON materials (course_id);
