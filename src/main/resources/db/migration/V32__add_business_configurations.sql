CREATE TABLE business_configurations (
  business_id   UUID PRIMARY KEY REFERENCES business_profiles(id) ON DELETE CASCADE,
  max_featured_products INTEGER,
  updated_by    UUID REFERENCES users(id),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
