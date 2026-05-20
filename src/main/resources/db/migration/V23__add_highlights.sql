CREATE TABLE highlights (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type         VARCHAR(20) NOT NULL,
    reference_id UUID        NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID        REFERENCES users(id)
);

CREATE INDEX idx_highlights_enabled_expires ON highlights(enabled, expires_at);

INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('highlights', 'Highlights Section', FALSE);
