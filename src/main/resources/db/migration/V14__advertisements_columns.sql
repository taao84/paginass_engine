--- Replace ad_category enum with the final set of values (SALE, JOB, EVENT).
--- V1 created the type as (SALE, JOB, HELP_WANTED, ANNOUNCEMENT); those two values
--- were never exposed and are removed here. PostgreSQL does not support DROP VALUE,
--- so a full type swap is required.
CREATE TYPE ad_category_new AS ENUM ('SALE', 'JOB', 'EVENT');

ALTER TABLE advertisements
    ALTER COLUMN category TYPE ad_category_new
    USING category::text::ad_category_new;

DROP TYPE ad_category;
ALTER TYPE ad_category_new RENAME TO ad_category;

--- Add image_urls for per-ad image storage
ALTER TABLE advertisements ADD COLUMN image_urls JSONB NOT NULL DEFAULT '[]';
UPDATE advertisements SET image_urls = '[]' WHERE image_urls IS NULL;

--- Add event_date for EVENT-category ads
ALTER TABLE advertisements ADD COLUMN event_date TIMESTAMP WITH TIME ZONE;

--- Index on expires_at for efficient active-ad queries
CREATE INDEX idx_ads_expires_at ON advertisements (expires_at DESC);

--- Register advertisements as a toggleable feature (disabled by default)
INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('advertisements', 'Clasificados', FALSE);
