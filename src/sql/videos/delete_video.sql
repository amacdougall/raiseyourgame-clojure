-- Deletes the video record with video-id.
DELETE FROM videos
  WHERE video_id = :video-id;
