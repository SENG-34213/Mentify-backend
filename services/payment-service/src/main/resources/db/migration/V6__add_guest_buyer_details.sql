ALTER TABLE payments
    ALTER COLUMN student_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS buyer_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS buyer_email VARCHAR(320),
    ADD COLUMN IF NOT EXISTS buyer_phone VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_payments_buyer_email
    ON payments (buyer_email);
