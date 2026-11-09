-- Capa 1: datos de referencia (Metadata de la Halo API). Permanentes, se refrescan rara vez.

CREATE TABLE maps (
    map_id     VARCHAR(255) PRIMARY KEY,   -- el "MapId" que llega en las partidas
    identity   UUID,                       -- "Identity" del ítem en la Metadata API
    name       VARCHAR(255) NOT NULL,
    synced_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE playlists (
    playlist_id          UUID PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    stats_classification VARCHAR(64),
    team_game            BOOLEAN,
    hidden               BOOLEAN,
    synced_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE seasons (
    season_id  UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    start_date TIMESTAMPTZ,
    synced_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE csr_designations (
    designation_id INTEGER PRIMARY KEY,
    name           VARCHAR(64) NOT NULL,
    synced_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Capa 3: hechos de análisis. Permanentes y SIN gamertag: una fila por jugador-en-partida,
-- identificada por (partida, índice del jugador). Aquí se apoyarán los winrates por líder,
-- los matchups y el meta, así que no se purga cuando se limpia la caché de jugadores.
CREATE TABLE match_participants (
    match_id               UUID    NOT NULL REFERENCES matches (match_id) ON DELETE CASCADE,
    player_index           INTEGER NOT NULL,
    team_id                INTEGER,
    leader_id              INTEGER NOT NULL,
    outcome                INTEGER NOT NULL,
    completed_match        BOOLEAN NOT NULL,
    duration_seconds       BIGINT,
    csr_tier_before        INTEGER,
    csr_designation_before INTEGER,
    mmr_before             DOUBLE PRECISION,
    PRIMARY KEY (match_id, player_index)
);

CREATE INDEX ix_match_participants_leader ON match_participants (leader_id);
