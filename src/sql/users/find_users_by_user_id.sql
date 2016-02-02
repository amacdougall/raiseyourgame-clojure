-- Selects all users with the supplied user-id. In practice, only one.
SELECT * FROM users
  WHERE user_id = :user-id;
