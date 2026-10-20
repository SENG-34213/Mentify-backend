-- Repair migration for databases that were started before V6 was available.
-- Keep this idempotent because some environments may already have part or all
-- of the generic resource-scope columns.
ALTER TABLE materials ADD COLUMN IF NOT EXISTS resource_type VARCHAR(40);
ALTER TABLE materials ADD COLUMN IF NOT EXISTS resource_id UUID;
ALTER TABLE materials ADD COLUMN IF NOT EXISTS owner_id UUID;
ALTER TABLE materials ADD COLUMN IF NOT EXISTS visibility VARCHAR(30);

UPDATE materials
SET resource_type = 'COURSE'
WHERE resource_type IS NULL;

UPDATE materials
SET resource_id = course_id
WHERE resource_id IS NULL;

UPDATE materials
SET owner_id = uploaded_by
WHERE owner_id IS NULL;

UPDATE materials
SET visibility = CASE
    WHEN material_purpose = 'SUBMISSION_ATTACHMENT' THEN 'OWNER_ONLY'
    ELSE 'COURSE_MEMBERS'
END
WHERE visibility IS NULL;

ALTER TABLE materials ALTER COLUMN resource_type SET DEFAULT 'COURSE';
ALTER TABLE materials ALTER COLUMN resource_type SET NOT NULL;
ALTER TABLE materials ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE materials ALTER COLUMN visibility SET DEFAULT 'COURSE_MEMBERS';
ALTER TABLE materials ALTER COLUMN visibility SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_materials_resource_type') THEN
        ALTER TABLE materials ADD CONSTRAINT chk_materials_resource_type CHECK (
            resource_type IN ('COURSE', 'MODULE', 'LESSON', 'ASSIGNMENT', 'SUBMISSION', 'EXAM', 'AI_KNOWLEDGE', 'OTHER')
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_materials_visibility') THEN
        ALTER TABLE materials ADD CONSTRAINT chk_materials_visibility CHECK (
            visibility IN ('COURSE_MEMBERS', 'OWNER_ONLY', 'TEACHER_ONLY', 'ADMIN_ONLY', 'INTERNAL')
        );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_materials_resource ON materials (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_materials_owner_visibility ON materials (owner_id, visibility);
