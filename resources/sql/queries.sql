-- name: create-user<!
-- creates a new user record, returning the entire inserted record.
-- postgres lets you specify a specific resultset to return, using the
-- RETURNING keyword, but YeSQL doesn't support this, and I don't care enough
-- to try to work around it.
INSERT INTO users (
  username,
  password,
  name,
  profile,
  email,
  user_level,
  created_at,
  updated_at
)
VALUES (
  :username,
  :password,
  :name,
  :profile,
  :email,
  :user_level,
  NOW(),
  NOW()
);

-- name: get-users
-- selects all users, regardless of level
SELECT * FROM users;

-- name: get-user-by-id
-- retrieve a user given the id.
SELECT * FROM users
  WHERE id = :id;

-- name: get-user-by-email
-- retrieve a user given the email.
SELECT * FROM users
  WHERE email = :email;

-- name: get-user-by-username
-- retrieve a user given the username.
SELECT * FROM users
  WHERE username = :username;

-- name: update-user!
-- update an existing user record
UPDATE users
  SET id = :id,
      username = :username,
      password = :password,
      name = :name,
      profile = :profile,
      email = :email,
      user_level = :user_level,
      last_login = :last_login,
      updated_at = NOW()
WHERE id = :id;

-- name: delete-user!
-- delete a user given the id
DELETE FROM users
WHERE id = :id
