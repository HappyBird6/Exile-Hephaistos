CREATE TABLE season.crawl_settings (
  id integer PRIMARY KEY CHECK (id = 1),
  version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
INSERT INTO season.crawl_settings(id) VALUES (1);
CREATE TABLE season.crawl_target (
  id uuid PRIMARY KEY,
  name varchar(80) NOT NULL,
  url varchar(1024) NOT NULL UNIQUE,
  enabled boolean NOT NULL,
  position integer NOT NULL UNIQUE CHECK (position >= 0 AND position < 20)
);
INSERT INTO season.crawl_target VALUES
 ('22222222-2222-4222-8222-222222222221','Currency','https://poe2db.tw/us/Currency',true,0),
 ('22222222-2222-4222-8222-222222222222','Amulets','https://poe2db.tw/us/Amulets',true,1);
CREATE TABLE season.crawl_run (
  id uuid PRIMARY KEY,
  status varchar(16) NOT NULL CHECK (status IN ('QUEUED','RUNNING','RAW_CAPTURED','FAILED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  started_at timestamptz,
  finished_at timestamptz,
  targets jsonb NOT NULL,
  sources jsonb,
  error_code varchar(40),
  source_count integer CHECK (source_count >= 0)
);
-- One active capture across all application instances, including concurrent requests.
CREATE UNIQUE INDEX crawl_run_single_active ON season.crawl_run ((true))
 WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX crawl_run_recent ON season.crawl_run (created_at DESC);
