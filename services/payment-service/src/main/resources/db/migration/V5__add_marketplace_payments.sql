ALTER TABLE payments
    ALTER COLUMN course_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS marketplace_product_id UUID,
    ADD COLUMN IF NOT EXISTS product_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_type VARCHAR(20) NOT NULL DEFAULT 'COURSE';

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_payment_type
        CHECK (payment_type IN ('COURSE', 'MARKETPLACE'));

CREATE INDEX IF NOT EXISTS idx_payments_marketplace_product_id
    ON payments (marketplace_product_id);

CREATE INDEX IF NOT EXISTS idx_payments_payment_type
    ON payments (payment_type);
