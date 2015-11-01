--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: annotations; Type: TABLE; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

CREATE TABLE annotations (
    annotation_id integer NOT NULL,
    video_id integer,
    user_id integer,
    active boolean DEFAULT true,
    timecode integer NOT NULL,
    text text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.annotations OWNER TO "raiseyourgame-dev";

--
-- Name: annotations_annotation_id_seq; Type: SEQUENCE; Schema: public; Owner: raiseyourgame-dev
--

CREATE SEQUENCE annotations_annotation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.annotations_annotation_id_seq OWNER TO "raiseyourgame-dev";

--
-- Name: annotations_annotation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: raiseyourgame-dev
--

ALTER SEQUENCE annotations_annotation_id_seq OWNED BY annotations.annotation_id;


--
-- Name: schema_migrations; Type: TABLE; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

CREATE TABLE schema_migrations (
    id bigint NOT NULL
);


ALTER TABLE public.schema_migrations OWNER TO "raiseyourgame-dev";

--
-- Name: users; Type: TABLE; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

CREATE TABLE users (
    user_id integer NOT NULL,
    active boolean DEFAULT true,
    username text NOT NULL,
    password text NOT NULL,
    name text,
    profile text,
    email text NOT NULL,
    user_level integer DEFAULT 0 NOT NULL,
    last_login timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.users OWNER TO "raiseyourgame-dev";

--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: raiseyourgame-dev
--

CREATE SEQUENCE users_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_user_id_seq OWNER TO "raiseyourgame-dev";

--
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: raiseyourgame-dev
--

ALTER SEQUENCE users_user_id_seq OWNED BY users.user_id;


--
-- Name: videos; Type: TABLE; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

CREATE TABLE videos (
    video_id integer NOT NULL,
    user_id integer,
    active boolean DEFAULT true,
    url text NOT NULL,
    length integer,
    title text NOT NULL,
    blurb text,
    description text,
    times_started integer DEFAULT 0 NOT NULL,
    times_completed integer DEFAULT 0 NOT NULL,
    times_upvoted integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.videos OWNER TO "raiseyourgame-dev";

--
-- Name: videos_video_id_seq; Type: SEQUENCE; Schema: public; Owner: raiseyourgame-dev
--

CREATE SEQUENCE videos_video_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.videos_video_id_seq OWNER TO "raiseyourgame-dev";

--
-- Name: videos_video_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: raiseyourgame-dev
--

ALTER SEQUENCE videos_video_id_seq OWNED BY videos.video_id;


--
-- Name: annotation_id; Type: DEFAULT; Schema: public; Owner: raiseyourgame-dev
--

ALTER TABLE ONLY annotations ALTER COLUMN annotation_id SET DEFAULT nextval('annotations_annotation_id_seq'::regclass);


--
-- Name: user_id; Type: DEFAULT; Schema: public; Owner: raiseyourgame-dev
--

ALTER TABLE ONLY users ALTER COLUMN user_id SET DEFAULT nextval('users_user_id_seq'::regclass);


--
-- Name: video_id; Type: DEFAULT; Schema: public; Owner: raiseyourgame-dev
--

ALTER TABLE ONLY videos ALTER COLUMN video_id SET DEFAULT nextval('videos_video_id_seq'::regclass);


--
-- Name: annotations_pkey; Type: CONSTRAINT; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

ALTER TABLE ONLY annotations
    ADD CONSTRAINT annotations_pkey PRIMARY KEY (annotation_id);


--
-- Name: schema_migrations_id_key; Type: CONSTRAINT; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

ALTER TABLE ONLY schema_migrations
    ADD CONSTRAINT schema_migrations_id_key UNIQUE (id);


--
-- Name: users_email_key; Type: CONSTRAINT; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: users_username_key; Type: CONSTRAINT; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: videos_pkey; Type: CONSTRAINT; Schema: public; Owner: raiseyourgame-dev; Tablespace: 
--

ALTER TABLE ONLY videos
    ADD CONSTRAINT videos_pkey PRIMARY KEY (video_id);


--
-- Name: annotations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: raiseyourgame-dev
--

ALTER TABLE ONLY annotations
    ADD CONSTRAINT annotations_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id);


--
-- Name: annotations_video_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: raiseyourgame-dev
--

ALTER TABLE ONLY annotations
    ADD CONSTRAINT annotations_video_id_fkey FOREIGN KEY (video_id) REFERENCES videos(video_id);


--
-- Name: videos_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: raiseyourgame-dev
--

ALTER TABLE ONLY videos
    ADD CONSTRAINT videos_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

