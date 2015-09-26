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
-- Selects all videos, starting at :start and returning :limit records.
SELECT * FROM videos;

-- name: find-videos-by-video-id
-- Selects all videos with :video_id. In practice, returns a set of one.
SELECT * FROM videos
  WHERE video_id = :video_id;

-- name: find-videos-by-user-id
-- Selects all videos with :user_id.
SELECT * FROM videos
  WHERE user_id = :user_id;

-- name: update-video!
-- Updates an existing video record. Use the entire desired video record as the
-- argument; the query will key on :video_id.
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
      updated_at = NOW()
  WHERE video_id = :video_id;

-- name: delete-video!
-- Deletes the video record with :video_id.
DELETE FROM videos
  WHERE video_id = :video_id;
