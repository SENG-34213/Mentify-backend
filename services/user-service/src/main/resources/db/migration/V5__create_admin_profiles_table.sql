CREATE TABLE IF NOT EXISTS admin_profiles (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id UUID NOT NULL UNIQUE,
    admin_code VARCHAR(50) UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE,
    nic VARCHAR(20),
    phone_number VARCHAR(20),
    CONSTRAINT fk_admin_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);
