-- Creates a new user record, returning the entire inserted record.
INSERT INTO users (
  username,
  password,
  name,
  profile,
  email,
  created_at,
  updated_at
) VALUES (
  :username,
  :password,
  :name,
  :profile,
  :email,
  NOW(),
  NOW()
);
