-- Enable UUID extension for primary keys
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Custom Enums for strict data integrity
CREATE TYPE business_type AS ENUM ('BRICK_AND_MORTAR', 'INFORMAL', 'GIG');
CREATE TYPE ad_category AS ENUM ('SALE', 'JOB', 'HELP_WANTED', 'ANNOUNCEMENT');

---
--- 1. USERS TABLE (OAuth2 Focused)
---
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    oauth_provider VARCHAR(50) NOT NULL, -- e.g., 'google', 'facebook'
    oauth_id VARCHAR(255) NOT NULL,      -- unique ID from provider
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(oauth_provider, oauth_id)
);

---
--- 2. BUSINESS PROFILES (The "Mini-Site" Data)
---
CREATE TABLE business_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL, -- for URLs: townsearch.com/biz/joes-pizza
    biz_type business_type NOT NULL,
    description TEXT,

    -- JSONB Columns for flexibility
    contact_info JSONB NOT NULL DEFAULT '{}', -- { "phone": "...", "whatsapp": "...", "address": "..." }
    tags JSONB NOT NULL DEFAULT '[]',         -- ["plumber", "pipe-repair", "24h"]
    image_urls JSONB NOT NULL DEFAULT '[]',   -- ["s3://bucket/img1.jpg", "s3://bucket/img2.jpg"]

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
--- 3. ADVERTISEMENTS (Classifieds)
---
CREATE TABLE advertisements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category ad_category NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    price NUMERIC(12, 2), -- Optional price
    tags JSONB NOT NULL DEFAULT '[]',
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
--- 4. BUSINESS NEWS (Promos & Updates)
---
CREATE TABLE business_news (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(512),
    published_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
--- INDEXES FOR SEARCH & PERFORMANCE
---

-- GIN Index for fast searching inside JSONB tag arrays
CREATE INDEX idx_biz_tags ON business_profiles USING GIN (tags);
CREATE INDEX idx_ads_tags ON advertisements USING GIN (tags);

-- Index for searching business info inside JSONB (e.g., searching by phone or city)
CREATE INDEX idx_biz_contact_info ON business_profiles USING GIN (contact_info);

-- Standard index for the slug (used in every mini-site page load)
CREATE INDEX idx_biz_slug ON business_profiles (slug);

-- B-Tree index for sorting news by date
CREATE INDEX idx_news_published ON business_news (published_at DESC);
