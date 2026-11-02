CREATE TABLE IF NOT EXISTS registration_id_counters (
    registration_role VARCHAR(20) NOT NULL,
    registration_year INTEGER NOT NULL,
    registration_month INTEGER NOT NULL,
    last_number INTEGER NOT NULL,
    CONSTRAINT pk_registration_id_counters
        PRIMARY KEY (registration_role, registration_year, registration_month),
    CONSTRAINT ck_registration_id_counter_month
        CHECK (registration_month BETWEEN 1 AND 12),
    CONSTRAINT ck_registration_id_counter_number
        CHECK (last_number BETWEEN 0 AND 99)
);
