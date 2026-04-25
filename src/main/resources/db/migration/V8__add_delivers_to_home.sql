ALTER TABLE business_profiles
    ADD COLUMN delivers_to_home BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill existing records: all previously registered businesses default to no home delivery
UPDATE business_profiles
SET delivers_to_home = FALSE
WHERE delivers_to_home IS NULL;
