CREATE TYPE user_role AS ENUM ('ADMIN', 'CONTRIBUTOR', 'OWNER');

ALTER TABLE users
    ADD COLUMN role user_role NOT NULL DEFAULT 'CONTRIBUTOR';

-- Make owner_id nullable (businesses start unclaimed) and change cascade to SET NULL
-- so deleting a user returns their businesses to unclaimed rather than deleting them
ALTER TABLE business_profiles
    DROP CONSTRAINT business_profiles_owner_id_fkey,
    ALTER COLUMN owner_id DROP NOT NULL,
    ADD CONSTRAINT business_profiles_owner_id_fkey
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE business_profiles
    ADD COLUMN is_claimed BOOLEAN NOT NULL DEFAULT FALSE;

-- Claim queue
CREATE TYPE claim_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE business_claims (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id  UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    claimant_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       claim_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at  TIMESTAMP WITH TIME ZONE
);

-- Prevent a user from having more than one open claim on the same business
CREATE UNIQUE INDEX uq_claims_pending_per_user_business
    ON business_claims (business_id, claimant_id)
    WHERE status = 'PENDING';
