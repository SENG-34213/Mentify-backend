ALTER TABLE marketplace_products
    ADD COLUMN IF NOT EXISTS thumbnail_material_id UUID;
