-- TREQ015: backfill existing OWNER users to CONTRIBUTOR.
-- The 'OWNER' value is kept in the user_role DB enum but is no longer written by the application.
UPDATE users SET role = 'CONTRIBUTOR' WHERE role = 'OWNER';
