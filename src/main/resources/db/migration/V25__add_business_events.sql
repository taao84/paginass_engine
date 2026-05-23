-- BREQ039: Link event advertisements to a business profile.
-- business_id is nullable so existing ads are unaffected and orphaned events
-- survive if their business is later deleted.

ALTER TABLE advertisements
    ADD COLUMN business_id UUID REFERENCES business_profiles(id) ON DELETE SET NULL;

UPDATE advertisements SET business_id = NULL WHERE business_id IS NULL;

CREATE INDEX idx_ads_business_id ON advertisements (business_id);

INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('business_events', 'Eventos de Negocio', FALSE);
