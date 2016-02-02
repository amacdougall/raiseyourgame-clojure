-- name: create-video!
-- creates a new video record, returning the entire inserted record.
INSERT INTO videos (
  user_id,
  url,
  title,
  blurb,
  description,
  created_at,
  updated_at
) VALUES (
  :user_id,
  :url,
  :title,
  :blurb,
  :description,
  NOW(),
  NOW()
);
