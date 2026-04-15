ALTER TABLE users
    ADD COLUMN email_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_token VARCHAR(255),
    ADD COLUMN token_expires_at   TIMESTAMP WITH TIME ZONE;

-- OAuth2 users have their email verified by the identity provider
UPDATE users SET email_verified = TRUE WHERE oauth_provider <> 'local';
