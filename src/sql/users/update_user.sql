-- Update an existing user record. Use the entire desired user record as the
-- argument; the query will key on user-id.
UPDATE users
  SET active = :active,
      username = :username,
      password = :password,
      name = :name,
      profile = :profile,
      email = :email,
      user_level = :user-level,
      last_login = :last-login,
      updated_at = NOW()
WHERE user_id = :user-id;
