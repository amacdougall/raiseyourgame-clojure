CREATE TABLE videos (
  video_id serial PRIMARY KEY,
  user_id integer REFERENCES users,
  active boolean DEFAULT true,
  url text NOT NULL,
  length integer,
  title text NOT NULL,
  blurb text,
  description text,
  times_started integer NOT NULL DEFAULT 0,
  times_completed integer NOT NULL DEFAULT 0,
  times_upvoted integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
)
