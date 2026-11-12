CREATE SCHEMA community;
CREATE TABLE community.post (
 id UUID PRIMARY KEY, author_id UUID REFERENCES app_user.account(id), browser_id UUID NOT NULL,
 source_id VARCHAR(160) NOT NULL, deletion_hash VARCHAR(100), game_version VARCHAR(32) NOT NULL,
 base_id VARCHAR(256) NOT NULL, title VARCHAR(160) NOT NULL DEFAULT '', body TEXT NOT NULL DEFAULT '',
 film JSONB NOT NULL, shared_step INTEGER NOT NULL CHECK(shared_step>=0), revision BIGINT NOT NULL DEFAULT 1,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX board_member_source ON community.post(author_id,source_id) WHERE author_id IS NOT NULL AND NOT deleted;
CREATE UNIQUE INDEX board_guest_source ON community.post(browser_id,source_id) WHERE author_id IS NULL AND NOT deleted;
CREATE INDEX board_newest ON community.post(created_at DESC,id) WHERE NOT deleted;
CREATE INDEX board_base_version ON community.post(base_id,game_version,created_at DESC) WHERE NOT deleted;
CREATE INDEX board_author ON community.post(author_id,created_at DESC) WHERE NOT deleted;
