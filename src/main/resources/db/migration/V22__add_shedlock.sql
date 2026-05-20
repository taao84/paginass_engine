CREATE TABLE shedlock (
    name       VARCHAR(64)              NOT NULL,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by  VARCHAR(255)             NOT NULL,
    PRIMARY KEY (name)
);

ALTER TABLE advertisements ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE advertisements SET active = TRUE;

CREATE TABLE job_runs (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name     VARCHAR(100) NOT NULL,
    triggered_by VARCHAR(20)  NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at  TIMESTAMPTZ,
    status       VARCHAR(10),
    message      TEXT
);
CREATE INDEX idx_job_runs_started_at ON job_runs(started_at DESC);

INSERT INTO app_features (feature_key, display_name, enabled)
VALUES ('scheduler', 'Scheduler Automático', TRUE);
