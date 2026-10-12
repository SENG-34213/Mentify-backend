CREATE TABLE material_audit_events (
    id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    material_id UUID,
    actor_id UUID,
    course_id UUID,
    outcome VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_material_audit_events PRIMARY KEY (id),
    CONSTRAINT chk_material_audit_action CHECK (
        action IN ('UPLOAD', 'DOWNLOAD_ACCESS_GENERATED', 'ARCHIVE', 'DELETE', 'CLEANUP')
    ),
    CONSTRAINT chk_material_audit_outcome_non_blank CHECK (length(trim(outcome)) > 0)
);

CREATE INDEX idx_material_audit_material_id ON material_audit_events (material_id);
CREATE INDEX idx_material_audit_action_created ON material_audit_events (action, created_at);
