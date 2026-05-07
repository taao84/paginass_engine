CREATE TABLE app_features (
    feature_key  VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO app_features (feature_key, display_name, enabled) VALUES
    ('classic_directory',      'Directorio clásico',        TRUE),
    ('directory_by_category',  'Directorio por categorías', TRUE),
    ('business_map',           'Mapa de negocios',          TRUE);
