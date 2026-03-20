-- Add tsvector column for full-text search
ALTER TABLE business_profiles
    ADD COLUMN search_vector tsvector;

-- Trigger function: rebuilds search_vector on every INSERT or UPDATE
-- Weight A = name (highest), B = description, C = tags, D = contact_info (lowest)
CREATE OR REPLACE FUNCTION update_business_search_vector()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('spanish', coalesce(NEW.name, '')),        'A') ||
        setweight(to_tsvector('spanish', coalesce(NEW.description, '')), 'B') ||
        setweight(to_tsvector('spanish',
            coalesce(regexp_replace(NEW.tags::text,         '[\[\]",]', ' ', 'g'), '')), 'C') ||
        setweight(to_tsvector('spanish',
            coalesce(regexp_replace(NEW.contact_info::text, '[{}",:_]', ' ', 'g'), '')), 'D');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_business_search_vector
    BEFORE INSERT OR UPDATE ON business_profiles
    FOR EACH ROW EXECUTE FUNCTION update_business_search_vector();

-- Backfill existing rows by firing the trigger on all of them
UPDATE business_profiles SET name = name;

-- GIN index for fast tsvector lookups
CREATE INDEX idx_biz_search_vector ON business_profiles USING GIN (search_vector);
