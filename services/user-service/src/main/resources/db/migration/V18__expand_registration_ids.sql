ALTER TABLE student_profiles
    ALTER COLUMN student_id TYPE VARCHAR(50);

ALTER TABLE registration_id_counters
    DROP CONSTRAINT IF EXISTS ck_registration_id_counter_number;

ALTER TABLE registration_id_counters
    ADD CONSTRAINT ck_registration_id_counter_number
        CHECK (last_number >= 0);
