ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'EDITOR';

CREATE TABLE business_profile_edits (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_profile_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    edited_by           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    snapshot            JSONB NOT NULL,
    edited_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biz_edits_profile ON business_profile_edits (business_profile_id, edited_at DESC);

-- NOTE: The app_features INSERT for 'business_edit_history' is deferred until BREQ008
-- completes and creates the app_features table.
