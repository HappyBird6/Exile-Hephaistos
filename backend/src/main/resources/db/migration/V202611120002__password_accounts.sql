CREATE TABLE app_user.password_identity (
  username VARCHAR(32) PRIMARY KEY,
  account_id UUID NOT NULL UNIQUE REFERENCES app_user.account(id) ON DELETE CASCADE,
  password_hash VARCHAR(100) NOT NULL
);
