CREATE TABLE pre_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(25) NOT NULL,
    role VARCHAR(10) NOT NULL,
    district VARCHAR(40) NOT NULL,
    city VARCHAR(100) NOT NULL,
    class_format VARCHAR(10) NOT NULL,
    grade VARCHAR(2) NOT NULL,
    contact_method VARCHAR(10) NOT NULL,
    consent_confirmed BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    seen BOOLEAN NOT NULL DEFAULT FALSE,
    admin_remark VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pre_registrations_role CHECK (role IN ('student', 'parent')),
    CONSTRAINT ck_pre_registrations_class_format CHECK (class_format IN ('online', 'onsite', 'either')),
    CONSTRAINT ck_pre_registrations_status CHECK (status IN ('NEW', 'IN_REVIEW', 'COMPLETED'))
);

CREATE INDEX idx_pre_registrations_created_at ON pre_registrations (created_at DESC);
CREATE INDEX idx_pre_registrations_status_seen ON pre_registrations (status, seen);

CREATE TABLE pre_registration_subjects (
    pre_registration_id UUID NOT NULL REFERENCES pre_registrations (id) ON DELETE CASCADE,
    subject_order INTEGER NOT NULL,
    subject VARCHAR(80) NOT NULL,
    PRIMARY KEY (pre_registration_id, subject_order)
);