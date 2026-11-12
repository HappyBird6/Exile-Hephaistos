CREATE TABLE community.comment (
 id UUID PRIMARY KEY, post_id UUID NOT NULL REFERENCES community.post(id) ON DELETE CASCADE,
 author_id UUID NOT NULL REFERENCES app_user.account(id), body TEXT NOT NULL,
 revision BIGINT NOT NULL DEFAULT 1, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX comment_post_page ON community.comment(post_id,created_at DESC,id);
