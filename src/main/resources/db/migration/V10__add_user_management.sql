ALTER TABLE users
    ADD COLUMN is_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE users SET is_enabled = TRUE;

ALTER TABLE users
    ADD COLUMN reset_token VARCHAR(255),
    ADD COLUMN reset_token_expires_at TIMESTAMP WITH TIME ZONE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'app_features') THEN
        INSERT INTO app_features (feature_key, display_name, enabled)
        VALUES ('admin_user_management', 'Gestión de usuarios (Admin)', TRUE)
        ON CONFLICT (feature_key) DO NOTHING;
    END IF;
END $$;
