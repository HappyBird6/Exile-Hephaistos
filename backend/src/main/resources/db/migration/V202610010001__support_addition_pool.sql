-- Additive goal-independent, version-namespaced cache. Existing source tables/data are preserved.
CREATE TABLE crafting.support_addition_pool (
  namespace varchar(64) NOT NULL,
  pool_key varchar(64) NOT NULL,
  payload jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(namespace, pool_key)
);
