CREATE EXTENSION IF NOT EXISTS unaccent;

UPDATE business_profiles
SET tags = (
    SELECT jsonb_agg(normalized_tag)
    FROM (
        SELECT DISTINCT lower(unaccent(tag_text)) AS normalized_tag
        FROM jsonb_array_elements_text(tags) AS tag_text
        WHERE tag_text IS NOT NULL AND trim(tag_text) <> ''
    ) subq
)
WHERE tags IS NOT NULL
  AND jsonb_array_length(tags) > 0;
