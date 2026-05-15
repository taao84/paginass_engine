-- General-purpose user activity event log.
-- Append-only — every user action on the platform is recorded here.
-- event_type values are defined in EventType.java (e.g. LIKE_BUSINESS, UNLIKE_BUSINESS).
-- entity_type values are defined in EntityType.java (e.g. BUSINESS, ADVERTISEMENT).
CREATE TABLE user_events (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type   VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(50) NOT NULL,
    entity_id    UUID        NOT NULL,
    metadata     JSONB,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_events_entity  ON user_events (entity_type, entity_id);
CREATE INDEX idx_user_events_user    ON user_events (user_id);

-- Current like state per (user, business) pair.
-- A row means the user currently likes that business.
-- Deleting the row means the user has unliked.
CREATE TABLE business_likes (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id  UUID        NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, business_id)
);

CREATE INDEX idx_business_likes_business ON business_likes (business_id);

-- Feature toggle — off by default.
INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('business_likes', 'Business Likes', FALSE);
