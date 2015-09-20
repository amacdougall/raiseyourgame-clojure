-- name: create-annotation<!
-- creates a new annotation record, returning the entire inserted record.
INSERT INTO annotation (
  video_id,
  user_id,
  text,
  created_at,
  updated_at
) VALUES (
  :video_id,
  :user_id,
  :text,
  NOW(),
  NOW()
);

-- name: get-annotations
-- selects all annotations
SELECT * FROM annotations;

-- name: get-annotation-by-annotation-id
-- retrieve a annotation given the annotation_id.
SELECT * FROM annotations
  WHERE annotation_id = :annotation_id;

-- name: get-annotations-by-video-id
-- retrieve all annotations for the supplied video
SELECT * FROM annotations
  WHERE video_id = :video_id;

-- name: update-annotation!
-- update an existing annotation record
UPDATE annotations
  SET video_id = :video_id,
      user_id = :user_id,
      active = :active,
      text = :text,
      updated_at = NOW()
  WHERE annotation_id = :annotation_id;

-- name: delete-annotation!
-- delete a annotation given the annotation_id
DELETE FROM annotations
  WHERE annotation_id = :annotation_id;
