CREATE INDEX idx_materials_title ON materials (title);
CREATE INDEX idx_materials_original_filename ON materials (original_filename);
CREATE INDEX idx_materials_course_status_created ON materials (course_id, status, created_at);
