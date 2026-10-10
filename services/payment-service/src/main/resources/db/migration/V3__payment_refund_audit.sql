ALTER TABLE payments
    ADD COLUMN stripe_refund_id VARCHAR(255),
    ADD COLUMN refunded_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN refund_reason VARCHAR(500);

CREATE UNIQUE INDEX uk_payments_stripe_refund_id
    ON payments (stripe_refund_id)
    WHERE stripe_refund_id IS NOT NULL;
