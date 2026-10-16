CREATE TABLE payments (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    paid_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payments_currency_uppercase CHECK (currency = upper(currency) AND length(currency) = 3),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payments_provider CHECK (provider IN ('STRIPE')),
    CONSTRAINT uk_payments_stripe_payment_intent_id UNIQUE (stripe_payment_intent_id)
);

CREATE INDEX idx_payments_student_id ON payments (student_id);
CREATE INDEX idx_payments_course_id ON payments (course_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);
