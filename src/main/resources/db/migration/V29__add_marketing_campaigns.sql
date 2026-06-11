-- BREQ045: Marketing Campaigns & Photography Contest

CREATE TABLE campaigns (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    campaign_type  VARCHAR(50)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    slug           VARCHAR(200) NOT NULL,
    image_subpath  TEXT,
    details        JSONB,
    start_datetime TIMESTAMPTZ,
    end_datetime   TIMESTAMPTZ  NOT NULL,
    created_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_campaigns_slug UNIQUE (slug)
);

CREATE INDEX idx_campaigns_slug   ON campaigns (slug);
CREATE INDEX idx_campaigns_status ON campaigns (status);

CREATE TABLE photo_contest_entries (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id   UUID         NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    photo_subpath VARCHAR(500) NOT NULL,
    disabled      BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_entry_per_user_per_contest UNIQUE (campaign_id, user_id)
);

UPDATE photo_contest_entries SET disabled = false;

CREATE INDEX idx_entries_campaign_id ON photo_contest_entries (campaign_id);

CREATE TABLE photo_contest_likes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id   UUID        NOT NULL REFERENCES photo_contest_entries(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_like_per_user_per_entry UNIQUE (entry_id, user_id)
);

CREATE INDEX idx_likes_entry_id ON photo_contest_likes (entry_id);

CREATE TABLE campaign_businesses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id   UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    business_id   UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    display_order INT  NOT NULL DEFAULT 0,
    CONSTRAINT uq_campaign_business UNIQUE (campaign_id, business_id)
);

CREATE INDEX idx_campaign_businesses_campaign_id ON campaign_businesses (campaign_id);

INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('campaigns', 'Campañas de Marketing', FALSE);
