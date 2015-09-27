-- name: create-annotation<!
-- Creates a new annotation record, returning the entire inserted record.
INSERT INTO annotations (
  video_id,
  user_id,
  text,
  timecode,
  created_at,
  updated_at
) VALUES (
  :video_id,
  :user_id,
  :text,
  :timecode,
  NOW(),
  NOW()
);

-- name: get-annotations
-- Selects all annotations ever.
SELECT * FROM annotations;

-- name: find-annotations-by-annotation-id
-- Selects all annotations which have :annotation_id. In practice, only one.
SELECT * FROM annotations
  WHERE annotation_id = :annotation_id;

-- name: find-annotations-by-video-id
-- Selects all annotations which have :video_id.
SELECT * FROM annotations
  WHERE video_id = :video_id;

-- name: update-annotation!
-- Updates an existing annotation record. Use the entire desired annotation
-- record as the argument; the query will key on :annotation_id.
UPDATE annotations
  SET video_id = :video_id,
      user_id = :user_id,
      active = :active,
      text = :text,
      timecode = :timecode,
      updated_at = NOW()
  WHERE annotation_id = :annotation_id;

-- name: delete-annotation!
-- Deletes the annotation record with :annotation_id.
DELETE FROM annotations
  WHERE annotation_id = :annotation_id;
