-- =====================================================================
-- Schema for: USERS, PROFILES, PHOTOS, LIKES, MATCHES, MESSAGES, REPORTS,
--             VERIFICATIONS, BANS
-- Notes:
-- - Uses UUID primary keys with gen_random_uuid() (pgcrypto).
-- - Uses PostGIS geography(Point,4326) for profile location (optional).
-- - Enforces one primary photo per profile.
-- - Enforces canonical match ordering (user_a < user_b) to prevent duplicates.
-- - Ensures message sender belongs to the match via trigger.
-- =====================================================================

BEGIN;

-- Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS postgis;   -- geography(Point,4326)

-- Enumerations
CREATE TYPE user_role AS ENUM ('user', 'moderator', 'admin');
CREATE TYPE verification_status AS ENUM ('pending', 'approved', 'rejected');
CREATE TYPE report_status AS ENUM ('open', 'reviewed', 'closed');

-- Utility: auto-update updated_at
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- USERS
CREATE TABLE users (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email         text NOT NULL UNIQUE,
  phone         text UNIQUE,
  password_hash text NOT NULL,
  role          user_role NOT NULL DEFAULT 'user',
  created_at    timestamptz NOT NULL DEFAULT NOW(),
  updated_at    timestamptz NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- PROFILES (one per user)
CREATE TABLE profiles (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid NOT NULL UNIQUE
                   REFERENCES users(id) ON DELETE CASCADE,
  display_name text,
  age          integer CHECK (age BETWEEN 13 AND 120),
  bio          text,
  gender       text,
  interests    text[],
  -- Optional location: WGS84 point (lon/lat)
  location     geography(Point, 4326),
  updated_at   timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX profiles_user_id_idx   ON profiles(user_id);
CREATE INDEX profiles_location_gix  ON profiles USING GIST (location);
CREATE TRIGGER trg_profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- PHOTOS
CREATE TABLE photos (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id  uuid NOT NULL
                 REFERENCES profiles(id) ON DELETE CASCADE,
  object_key  text NOT NULL,        -- e.g., S3/MinIO key
  is_primary  boolean NOT NULL DEFAULT false,
  uploaded_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX photos_profile_id_idx ON photos(profile_id);
-- At most one primary photo per profile
CREATE UNIQUE INDEX photos_one_primary_per_profile
  ON photos(profile_id)
  WHERE is_primary;

-- LIKES
CREATE TABLE likes (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  from_user  uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  to_user    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  positive   boolean NOT NULL DEFAULT true,  -- true=like, false=dislike
  created_at timestamptz NOT NULL DEFAULT NOW(),
  CONSTRAINT likes_no_self CHECK (from_user <> to_user),
  CONSTRAINT likes_unique_pair UNIQUE (from_user, to_user)
);
CREATE INDEX likes_to_user_idx             ON likes(to_user);
CREATE INDEX likes_to_user_positive_idx    ON likes(to_user) WHERE positive;

-- MATCHES (unique unordered pair; use canonical ordering user_a < user_b)
CREATE TABLE matches (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_a     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_b     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT NOW(),
  CONSTRAINT matches_distinct_users CHECK (user_a <> user_b),
  CONSTRAINT matches_canonical_order CHECK (user_a < user_b),
  CONSTRAINT matches_unique_pair UNIQUE (user_a, user_b)
);
CREATE INDEX matches_user_a_idx ON matches(user_a);
CREATE INDEX matches_user_b_idx ON matches(user_b);

-- MESSAGES (belongs to a match; sent by a user)
CREATE TABLE messages (
  id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id  uuid NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
  sender_id uuid NOT NULL REFERENCES users(id),   -- who sent it
  body      text NOT NULL,
  sent_at   timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX messages_match_id_idx  ON messages(match_id);
CREATE INDEX messages_sender_id_idx ON messages(sender_id);
CREATE INDEX messages_sent_at_idx   ON messages(sent_at);

-- Ensure the sender participates in the match
CREATE OR REPLACE FUNCTION enforce_sender_in_match() RETURNS trigger AS $$
DECLARE
  a uuid; b uuid;
BEGIN
  SELECT user_a, user_b INTO a, b FROM matches WHERE id = NEW.match_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Match % not found', NEW.match_id;
  END IF;
  IF NEW.sender_id <> a AND NEW.sender_id <> b THEN
    RAISE EXCEPTION 'Sender % is not a participant in match %',
      NEW.sender_id, NEW.match_id;
  END IF;
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_messages_sender_in_match
  BEFORE INSERT OR UPDATE ON messages
  FOR EACH ROW EXECUTE PROCEDURE enforce_sender_in_match();

-- REPORTS (USERS file reports against USERS)
CREATE TABLE reports (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reported_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reason           text NOT NULL,
  status           report_status NOT NULL DEFAULT 'open',
  created_at       timestamptz NOT NULL DEFAULT NOW(),
  reviewed_at      timestamptz,
  CONSTRAINT reports_not_self CHECK (reporter_user_id <> reported_user_id)
);
CREATE INDEX reports_reported_user_idx ON reports(reported_user_id);
CREATE INDEX reports_status_idx        ON reports(status);

-- VERIFICATIONS (USERS pass verifications)
CREATE TABLE verifications (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  method       text,                      -- e.g., 'selfie', 'sms'
  status       verification_status NOT NULL DEFAULT 'pending',
  requested_at timestamptz NOT NULL DEFAULT NOW(),
  verified_at  timestamptz,
  metadata     jsonb                      -- optional extra details
);
CREATE INDEX verifications_user_idx   ON verifications(user_id);
CREATE INDEX verifications_status_idx ON verifications(status);

-- BANS (USERS receive bans)
CREATE TABLE bans (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reason     text,
  created_at timestamptz NOT NULL DEFAULT NOW(),
  starts_at  timestamptz NOT NULL DEFAULT NOW(),
  ends_at    timestamptz,
  CONSTRAINT bans_valid_interval CHECK (ends_at IS NULL OR ends_at > starts_at)
);
CREATE INDEX bans_user_idx   ON bans(user_id);
-- Quickly find active bans
CREATE INDEX bans_active_idx ON bans(user_id)
  WHERE ends_at IS NULL OR ends_at > NOW();

COMMIT;
