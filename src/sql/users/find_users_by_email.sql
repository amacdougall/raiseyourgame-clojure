-- Selects all users with the supplied email. In practice, only one.
SELECT * FROM users
  WHERE email = :email;
