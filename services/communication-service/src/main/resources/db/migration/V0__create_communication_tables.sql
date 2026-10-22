CREATE TABLE IF NOT EXISTS communication_groups (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_id UUID NOT NULL, name VARCHAR(150) NOT NULL, description TEXT, status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_communication_groups_course_id UNIQUE (course_id)
);

CREATE TABLE IF NOT EXISTS communication_settings (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id UUID NOT NULL, read_receipts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_visible BOOLEAN NOT NULL DEFAULT TRUE, profile_photo_visible BOOLEAN NOT NULL DEFAULT TRUE,
    group_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE, direct_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_communication_settings_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS group_members (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    group_id UUID NOT NULL REFERENCES communication_groups(id), user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL, joined_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    group_id UUID NOT NULL REFERENCES communication_groups(id), sender_id UUID NOT NULL,
    content VARCHAR(5000), type VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP NOT NULL, edited_at TIMESTAMP, deleted_at TIMESTAMP, deleted_by UUID
);

CREATE TABLE IF NOT EXISTS message_hidden_for_user (
    id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by UUID, updated_by UUID, is_active BOOLEAN NOT NULL DEFAULT TRUE,
    message_id UUID NOT NULL REFERENCES messages(id), user_id UUID NOT NULL, hidden_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_hidden_message_user UNIQUE (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_communication_groups_course_id ON communication_groups(course_id);
CREATE INDEX IF NOT EXISTS idx_communication_settings_user ON communication_settings(user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_group_user ON group_members(group_id, user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user_active ON group_members(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_message_group_sent_at ON messages(group_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_hidden_message_user ON message_hidden_for_user(user_id, message_id);
