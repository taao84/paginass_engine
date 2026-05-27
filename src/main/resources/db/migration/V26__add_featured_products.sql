-- BREQ040: Productos Destacados — per-business highlighted product showcase.

CREATE TABLE featured_products (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    price         NUMERIC(12, 2),
    image_subpath VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_featured_products_business_id ON featured_products (business_id);

INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('featured_products', 'Productos Destacados', FALSE);
