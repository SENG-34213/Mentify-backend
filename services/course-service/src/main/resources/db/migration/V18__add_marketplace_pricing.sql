ALTER TABLE marketplace_products
    ADD COLUMN IF NOT EXISTS is_free BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2);

ALTER TABLE marketplace_products
    DROP CONSTRAINT IF EXISTS chk_marketplace_products_discount;

ALTER TABLE marketplace_products
    ADD CONSTRAINT chk_marketplace_products_discount
        CHECK (discount_percent IS NULL OR (discount_percent >= 0 AND discount_percent <= 100));
