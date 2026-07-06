ALTER TABLE photo_contest_entries
    ADD COLUMN winner_rank INTEGER,
    ADD COLUMN notified_at TIMESTAMPTZ;

CREATE UNIQUE INDEX idx_photo_contest_entries_campaign_winner_rank
    ON photo_contest_entries (campaign_id, winner_rank)
    WHERE winner_rank IS NOT NULL;
