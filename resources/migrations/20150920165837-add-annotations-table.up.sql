CREATE TABLE annotations (
  annotation_id serial PRIMARY KEY,
  video_id integer REFERENCES videos,
  user_id integer REFERENCES users,
  active boolean DEFAULT true,
  timecode integer NOT NULL,
  text text NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
)
