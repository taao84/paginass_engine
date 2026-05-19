CREATE TABLE app_news (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(255) NOT NULL,
    description   TEXT         NOT NULL,
    image_url     VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by_id UUID         NOT NULL REFERENCES users(id)
);

INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('app_news', 'Novedades', FALSE);
