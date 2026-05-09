CREATE TABLE banner_history (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    sub_path    TEXT        NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_banner_history PRIMARY KEY (id)
);
