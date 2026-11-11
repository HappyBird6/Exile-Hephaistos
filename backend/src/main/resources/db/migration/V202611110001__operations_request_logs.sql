CREATE SCHEMA operations;
CREATE TABLE operations.request_log (
  id uuid PRIMARY KEY,
  occurred_at timestamptz NOT NULL,
  method varchar(12) NOT NULL,
  path varchar(256) NOT NULL,
  route varchar(256) NOT NULL,
  status integer NOT NULL,
  duration_ms bigint NOT NULL,
  client_ip varchar(64) NOT NULL,
  visitor_id varchar(36),
  page_views integer NOT NULL DEFAULT 0,
  error_type varchar(120),
  user_agent varchar(256)
);
CREATE INDEX request_log_time ON operations.request_log(occurred_at DESC, id);
CREATE INDEX request_log_ip_time ON operations.request_log(client_ip, occurred_at DESC);
CREATE INDEX request_log_visitor_time ON operations.request_log(visitor_id, occurred_at DESC);
CREATE INDEX request_log_route_time ON operations.request_log(route, occurred_at DESC);
CREATE INDEX request_log_problem_time ON operations.request_log(status, occurred_at DESC) WHERE status >= 400;
CREATE TABLE operations.log_archive (
  id uuid PRIMARY KEY,
  filename varchar(120) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL,
  first_event_at timestamptz NOT NULL,
  last_event_at timestamptz NOT NULL,
  row_count integer NOT NULL,
  bytes bigint NOT NULL
);
