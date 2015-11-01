-- name: create-user<!
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

-- name: get-users
-- Selects all users, regardless of level.
SELECT * FROM users;

-- name: find-users-by-user-id
-- Selects all users with :user_id. In practice, only one.
SELECT * FROM users
  WHERE user_id = :user_id;

-- name: find-users-by-email
-- Selects all users with :email. In practice, only one.
SELECT * FROM users
  WHERE email = :email;

-- name: find-users-by-username
-- Selects all users with :username. In practice, only one.
SELECT * FROM users
  WHERE username = :username;

-- name: update-user!
-- Update an existing user record. Use the entire desired user record as the
-- argument; the query will key on :user_id.
UPDATE users
  SET active = :active,
      username = :username,
      password = :password,
      name = :name,
      profile = :profile,
      email = :email,
      user_level = :user_level,
      last_login = :last_login,
      updated_at = NOW()
WHERE user_id = :user_id;
