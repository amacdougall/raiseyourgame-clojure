-- Selects all users with the supplied username. In practice, only one.
SELECT * FROM users
  WHERE username = :username;
