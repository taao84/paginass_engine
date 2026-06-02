ALTER TABLE business_profiles
    ADD COLUMN banner_url    TEXT,
    ADD COLUMN thumbnail_url TEXT;

UPDATE business_profiles
SET    banner_url = image_urls ->> 0
WHERE  jsonb_array_length(image_urls) > 0;
