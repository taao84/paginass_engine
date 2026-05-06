ALTER TABLE business_profiles
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

-- Existing records have no location; null is the valid default and is handled in Java code.
