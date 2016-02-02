-- Updates an existing video record. Use the entire desired video record as the
-- argument; the query will key on video-id.
UPDATE videos
  SET user_id = :user-id,
      active = :active,
      url = :url,
      length = :length,
      title = :title,
      blurb = :blurb,
      description = :description,
      times_started = :times-started,
      times_completed = :times-completed,
      times_upvoted = :times-upvoted,
      updated_at = NOW()
  WHERE video_id = :video-id;
