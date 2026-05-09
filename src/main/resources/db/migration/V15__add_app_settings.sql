CREATE TABLE app_settings (
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_settings PRIMARY KEY (setting_key)
);
