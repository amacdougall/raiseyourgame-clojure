CREATE TABLE users (
  id serial PRIMARY KEY,
  username text UNIQUE NOT NULL,
  password text NOT NULL,
  name text,
  profile text,
  email text UNIQUE NOT NULL,
  user_level integer NOT NULL DEFAULT 0,
  last_login timestamptz DEFAULT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
)
