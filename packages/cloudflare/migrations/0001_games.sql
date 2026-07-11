PRAGMA foreign_keys = ON;

CREATE TABLE games (
  id TEXT PRIMARY KEY,
  owner_id TEXT NOT NULL,
  name TEXT NOT NULL,
  title TEXT NOT NULL,
  yaml TEXT NOT NULL,
  revision INTEGER NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('draft', 'published')),
  published_revision INTEGER,
  write_token TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX games_owner_updated ON games(owner_id, updated_at DESC);

CREATE TABLE game_revisions (
  game_id TEXT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  revision INTEGER NOT NULL,
  yaml TEXT NOT NULL,
  created_at TEXT NOT NULL,
  published_at TEXT,
  PRIMARY KEY (game_id, revision)
);
