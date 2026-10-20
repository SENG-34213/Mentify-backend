-- The communication service currently uses Hibernate schema update in development.
-- Apply this migration before switching production to validate/none.
ALTER TABLE messages ALTER COLUMN content DROP NOT NULL;

CREATE TABLE IF NOT EXISTS message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    material_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum VARCHAR(128),
    attachment_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_message_attachments_message
        FOREIGN KEY (message_id) REFERENCES messages(id),
    CONSTRAINT uk_message_attachment_material UNIQUE (material_id)
);

CREATE INDEX IF NOT EXISTS idx_message_attachments_message_id
    ON message_attachments(message_id);
