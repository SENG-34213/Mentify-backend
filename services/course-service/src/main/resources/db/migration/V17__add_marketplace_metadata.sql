ALTER TABLE marketplace_products
    ADD COLUMN IF NOT EXISTS material_owner_id UUID,
    ADD COLUMN IF NOT EXISTS material_owner_name VARCHAR(180),
    ADD COLUMN IF NOT EXISTS course_domain VARCHAR(120);
