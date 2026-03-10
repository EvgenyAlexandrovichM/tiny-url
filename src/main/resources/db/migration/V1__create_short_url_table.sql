CREATE SEQUENCE short_url_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE short_url (
    id BIGINT PRIMARY KEY DEFAULT nextval('short_url_id_seq'),
    short_code VARCHAR(16) NOT NULL UNIQUE,
    alias VARCHAR(64) UNIQUE,
    original_url TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_short_url_expires_at ON short_url(expires_at);
CREATE INDEX idx_short_url_created_at ON short_url(created_at);
