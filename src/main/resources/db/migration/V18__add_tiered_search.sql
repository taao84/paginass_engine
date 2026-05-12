-- Add unstemmed tsvector for precise prefix search (used by the tiered-search feature flag)
ALTER TABLE business_profiles
    ADD COLUMN search_vector_simple tsvector;

-- Update trigger to populate both the Spanish-stemmed and the simple (unstemmed) vectors
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

    NEW.search_vector_simple :=
        setweight(to_tsvector('simple', coalesce(NEW.name, '')),        'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'B') ||
        setweight(to_tsvector('simple',
            coalesce(regexp_replace(NEW.tags::text,         '[\[\]",]', ' ', 'g'), '')), 'C') ||
        setweight(to_tsvector('simple',
            coalesce(regexp_replace(NEW.contact_info::text, '[{}",:_]', ' ', 'g'), '')), 'D');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Backfill existing rows
UPDATE business_profiles SET name = name;

-- GIN index for fast simple-vector lookups
CREATE INDEX idx_biz_search_vector_simple ON business_profiles USING GIN (search_vector_simple);

-- Feature flag: disabled by default — enable explicitly in prod once validated
INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('tiered_search', 'Búsqueda por niveles (literales primero, luego stems)', FALSE);
