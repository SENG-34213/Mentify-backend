-- Material Service owns the purpose constraint. Extend it for Exam Service uploads.
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
            'OTHER'
        )
    );
