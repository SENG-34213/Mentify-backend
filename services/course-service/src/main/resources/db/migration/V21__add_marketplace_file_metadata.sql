ALTER TABLE marketplace_products
    ADD COLUMN IF NOT EXISTS file_format VARCHAR(20),
    ADD COLUMN IF NOT EXISTS page_count INTEGER;

ALTER TABLE marketplace_products
    DROP CONSTRAINT IF EXISTS chk_marketplace_products_page_count;

ALTER TABLE marketplace_products
    ADD CONSTRAINT chk_marketplace_products_page_count
        CHECK (page_count IS NULL OR page_count > 0);
