-- BREQ042: distinguish global settings from feature-specific ones.

ALTER TABLE app_settings
    ADD COLUMN is_global BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE app_settings
SET is_global = FALSE;
