-- Selects all videos with video-id. In practice, returns a set of one.
SELECT * FROM videos
  WHERE video_id = :video-id;
