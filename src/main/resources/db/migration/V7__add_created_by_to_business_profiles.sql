ALTER TABLE business_profiles
    ADD COLUMN created_by UUID REFERENCES users(id) ON DELETE SET NULL;
