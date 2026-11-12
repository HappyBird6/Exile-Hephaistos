ALTER TABLE community.post ADD COLUMN downloads BIGINT NOT NULL DEFAULT 0 CHECK(downloads>=0), ADD COLUMN recommendations BIGINT NOT NULL DEFAULT 0 CHECK(recommendations>=0);
CREATE INDEX board_popular ON community.post(downloads DESC,created_at DESC,id) WHERE NOT deleted;
CREATE TABLE community.download_entry(id UUID PRIMARY KEY,post_id UUID NOT NULL REFERENCES community.post(id) ON DELETE CASCADE,account_id UUID REFERENCES app_user.account(id));
CREATE UNIQUE INDEX download_member ON community.download_entry(post_id,account_id) WHERE account_id IS NOT NULL;
CREATE INDEX download_post ON community.download_entry(post_id);
CREATE TABLE community.download_browser(post_id UUID NOT NULL REFERENCES community.post(id) ON DELETE CASCADE,browser_id UUID NOT NULL,entry_id UUID NOT NULL REFERENCES community.download_entry(id) ON DELETE CASCADE,PRIMARY KEY(post_id,browser_id));
CREATE TABLE community.recommendation(post_id UUID NOT NULL REFERENCES community.post(id) ON DELETE CASCADE,account_id UUID NOT NULL REFERENCES app_user.account(id),PRIMARY KEY(post_id,account_id));
