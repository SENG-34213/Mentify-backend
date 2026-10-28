CREATE TABLE announcements (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    audience VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    publish_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_announcements_audience CHECK (audience IN ('ALL', 'STUDENT', 'TEACHER', 'ADMIN')),
    CONSTRAINT ck_announcements_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_announcements_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_announcements_expiry_after_publish CHECK (expires_at IS NULL OR publish_at IS NULL OR expires_at > publish_at)
);

CREATE INDEX ix_announcements_visible
    ON announcements (status, publish_at, expires_at, created_at DESC);

CREATE TABLE announcement_reads (
    id UUID PRIMARY KEY,
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    read_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_announcement_reads_announcement_user UNIQUE (announcement_id, user_id)
);

CREATE INDEX ix_announcement_reads_user
    ON announcement_reads (user_id, read_at DESC);
