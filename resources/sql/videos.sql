-- name: create-video<!
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

-- name: get-videos
-- selects all videos
SELECT * FROM videos;

-- name: get-video-by-video-id
-- retrieve a video given the video_id.
SELECT * FROM videos
  WHERE video_id = :video_id;

-- name: update-video!
-- update an existing video record
UPDATE videos
  SET user_id = :user_id,
      active = :active,
      url = :url,
      length = :length,
      title = :title,
      blurb = :blurb,
      description = :description,
      times_started = :times_started,
      times_completed = :times_completed,
      times_upvoted = :times_upvoted,
      created_at = :created_at,
      updated_at = :updated_at
  WHERE video_id = :video_id;

-- name: delete-video!
-- delete a video given the video_id
DELETE FROM videos
  WHERE video_id = :video_id;
