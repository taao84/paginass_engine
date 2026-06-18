CREATE TABLE contact_settings (
    id                INTEGER PRIMARY KEY DEFAULT 1,
    page_description  TEXT,
    contact_email     VARCHAR(255),
    contact_phone     VARCHAR(50),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        UUID REFERENCES users(id),
    CONSTRAINT single_row CHECK (id = 1)
);

INSERT INTO contact_settings (id) VALUES (1);

CREATE TABLE contact_messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject      VARCHAR(200) NOT NULL,
    reply_email  VARCHAR(255) NOT NULL,
    message      TEXT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at      TIMESTAMPTZ,
    read_by      UUID REFERENCES users(id)
);

INSERT INTO app_features (feature_key, display_name, enabled) VALUES ('contact_page', 'Página de contacto', FALSE);
