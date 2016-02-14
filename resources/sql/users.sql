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
-- Selects all users, beginning at :offset, limited to :limit results, ordered
-- by :order_by. Uses the default sort order.
SELECT * FROM users LIMIT :limit OFFSET :offset;

-- name: get-users-order-by-username-asc
-- As get-users, but orders by username, ascending.
SELECT * FROM users ORDER BY username ASC OFFSET :offset LIMIT :limit;

-- name: get-users-order-by-username-desc
-- As get-users, but orders by username, descending.
SELECT * FROM users ORDER BY username DESC OFFSET :offset LIMIT :limit;

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
