-- TREQ015: backfill existing OWNER users to CONTRIBUTOR.
-- The 'OWNER' value is kept in the user_role DB enum but is no longer written by the application.
UPDATE users SET role = 'CONTRIBUTOR' WHERE role = 'OWNER';

-- BREQ027: feature toggle for contributor self-registration of own business.
INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('contributor_business_creation', 'Registro de negocio propio', false)
ON CONFLICT (feature_key) DO NOTHING;
