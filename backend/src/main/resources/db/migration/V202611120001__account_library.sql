CREATE TABLE app_user.account (
  id UUID PRIMARY KEY,
  google_subject VARCHAR(255) UNIQUE,
  display_name VARCHAR(160) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE app_user.library_document (
  account_id UUID NOT NULL REFERENCES app_user.account(id) ON DELETE CASCADE,
  kind VARCHAR(24) NOT NULL CHECK (kind IN ('session', 'base-favorite', 'material-favorites')),
  document_id VARCHAR(160) NOT NULL,
  version BIGINT NOT NULL CHECK (version > 0),
  content JSONB,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (account_id, kind, document_id)
);
-- Null content is a deletion marker: stale clients cannot resurrect a deleted document.
CREATE INDEX library_document_account_updated ON app_user.library_document(account_id, updated_at);
