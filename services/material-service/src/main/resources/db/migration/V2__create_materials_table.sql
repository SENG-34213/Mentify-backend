CREATE TABLE materials (
    id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    material_type VARCHAR(30) NOT NULL,
    material_purpose VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    uploaded_by UUID NOT NULL,
    checksum VARCHAR(128),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_materials PRIMARY KEY (id),
    CONSTRAINT uk_materials_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_materials_title_non_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT chk_materials_original_filename_non_blank CHECK (length(trim(original_filename)) > 0),
    CONSTRAINT chk_materials_storage_key_non_blank CHECK (length(trim(storage_key)) > 0),
    CONSTRAINT chk_materials_storage_key_not_url CHECK (
        storage_key NOT LIKE 'http://%' AND storage_key NOT LIKE 'https://%'
    ),
    CONSTRAINT chk_materials_content_type_non_blank CHECK (length(trim(content_type)) > 0),
    CONSTRAINT chk_materials_file_size_positive CHECK (file_size > 0),
    CONSTRAINT chk_materials_type CHECK (
        material_type IN ('PDF', 'DOCUMENT', 'PRESENTATION', 'SPREADSHEET', 'IMAGE', 'VIDEO', 'AUDIO', 'ARCHIVE', 'OTHER')
    ),
    CONSTRAINT chk_materials_purpose CHECK (
        material_purpose IN (
            'LEARNING_MATERIAL',
            'ASSIGNMENT_ATTACHMENT',
            'SUBMISSION_ATTACHMENT',
            'AI_SOURCE',
            'ANNOUNCEMENT_ATTACHMENT',
            'OTHER'
        )
    ),
    CONSTRAINT chk_materials_status CHECK (
        status IN ('UPLOADED', 'PROCESSING', 'AVAILABLE', 'ARCHIVED', 'DELETED', 'FAILED')
    )
);

CREATE INDEX idx_materials_uploaded_by ON materials (uploaded_by);
CREATE INDEX idx_materials_status ON materials (status);
CREATE INDEX idx_materials_type ON materials (material_type);
CREATE INDEX idx_materials_purpose ON materials (material_purpose);
CREATE INDEX idx_materials_checksum ON materials (checksum);
CREATE INDEX idx_materials_created_at ON materials (created_at);
