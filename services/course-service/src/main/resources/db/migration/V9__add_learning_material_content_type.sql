ALTER TABLE learning_materials
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255);
