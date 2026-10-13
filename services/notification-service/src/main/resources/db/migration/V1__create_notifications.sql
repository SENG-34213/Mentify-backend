CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    recipient_user_id VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(320),
    type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    email_status VARCHAR(20) NOT NULL,
    email_attempts INTEGER NOT NULL DEFAULT 0,
    next_email_attempt_at TIMESTAMPTZ,
    last_email_error VARCHAR(1000),
    CONSTRAINT uq_notifications_event_recipient UNIQUE (event_id, recipient_user_id),
    CONSTRAINT ck_notifications_email_status
        CHECK (email_status IN ('NOT_REQUESTED', 'PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX ix_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX ix_notifications_recipient_unread
    ON notifications (recipient_user_id, read_at)
    WHERE read_at IS NULL;
CREATE INDEX ix_notifications_pending_email
    ON notifications (next_email_attempt_at, created_at)
    WHERE email_status = 'PENDING';
