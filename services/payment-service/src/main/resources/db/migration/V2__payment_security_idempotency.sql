ALTER TABLE payments
    ADD COLUMN stripe_client_secret VARCHAR(500),
    ADD COLUMN payment_operation_key VARCHAR(255);

UPDATE payments
SET payment_operation_key = student_id::text || '|' || course_id::text || '|' || currency || '|' || amount::text
WHERE payment_operation_key IS NULL;

ALTER TABLE payments
    ALTER COLUMN payment_operation_key SET NOT NULL;

CREATE UNIQUE INDEX uk_payments_pending_operation
    ON payments (payment_operation_key)
    WHERE status = 'PENDING';

CREATE TABLE processed_stripe_webhook_events (
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_processed_stripe_webhook_events PRIMARY KEY (event_id)
);

CREATE INDEX idx_processed_stripe_webhook_events_payment_intent
    ON processed_stripe_webhook_events (stripe_payment_intent_id);
