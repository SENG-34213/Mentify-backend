-- Keep database constraints aligned with MaterialPurpose and MaterialResourceType.
ALTER TABLE materials DROP CONSTRAINT IF EXISTS chk_materials_purpose;

ALTER TABLE materials
    ADD CONSTRAINT chk_materials_purpose CHECK (
        material_purpose IN (
            'LEARNING_MATERIAL',
            'ASSIGNMENT_ATTACHMENT',
            'SUBMISSION_ATTACHMENT',
            'EXAM_ATTACHMENT',
            'AI_SOURCE',
            'ANNOUNCEMENT_ATTACHMENT',
            'COMMUNICATION_ATTACHMENT',
            'MARKETPLACE_PRODUCT',
            'OTHER'
        )
    );

ALTER TABLE materials DROP CONSTRAINT IF EXISTS chk_materials_resource_type;

ALTER TABLE materials
    ADD CONSTRAINT chk_materials_resource_type CHECK (
        resource_type IN (
            'COURSE',
            'MODULE',
            'LESSON',
            'ASSIGNMENT',
            'SUBMISSION',
            'EXAM',
            'AI_KNOWLEDGE',
            'MARKETPLACE_PRODUCT',
            'OTHER'
        )
    );
