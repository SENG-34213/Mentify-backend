CREATE TABLE IF NOT EXISTS marketplace_products (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    material_id UUID NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    short_description VARCHAR(500),
    description TEXT,
    category VARCHAR(40) NOT NULL,
    subject VARCHAR(120),
    grade_id UUID,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'LKR',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    thumbnail_url VARCHAR(500),
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketplace_products_status ON marketplace_products(status);
CREATE INDEX IF NOT EXISTS idx_marketplace_products_category ON marketplace_products(category);
CREATE INDEX IF NOT EXISTS idx_marketplace_products_grade ON marketplace_products(grade_id);
