-- Tablas del MVP: jugadores, partidas y la participación de cada jugador en una partida.
-- Flyway ejecuta este script dentro del esquema configurado (spring.flyway.default-schema).

CREATE TABLE players (
    id          BIGSERIAL PRIMARY KEY,
    gamer_tag   VARCHAR(64) NOT NULL,
    last_sync   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

-- El gamertag es único sin distinguir mayúsculas/minúsculas.
CREATE UNIQUE INDEX ux_players_gamer_tag_lower ON players (lower(gamer_tag));

CREATE TABLE matches (
    match_id    UUID PRIMARY KEY,
    match_type  INTEGER NOT NULL,
    game_mode   INTEGER NOT NULL,
    season_id   UUID,
    playlist_id UUID,
    map_id      VARCHAR(255),
    start_date  TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_matches_start_date ON matches (start_date DESC);

CREATE TABLE player_matches (
    id                      BIGSERIAL PRIMARY KEY,
    player_id               BIGINT  NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    match_id                UUID    NOT NULL REFERENCES matches (match_id),
    leader_id               INTEGER NOT NULL,
    team_id                 INTEGER,
    player_index            INTEGER,
    team_player_index       INTEGER,
    completed_match         BOOLEAN NOT NULL,
    outcome                 INTEGER NOT NULL,
    duration_seconds        BIGINT,
    csr_tier_before         INTEGER,
    csr_designation_before  INTEGER,
    csr_tier_after          INTEGER,
    csr_designation_after   INTEGER,
    mmr_before              DOUBLE PRECISION,
    mmr_after               DOUBLE PRECISION,
    xp_before               INTEGER,
    xp_after                INTEGER,
    -- Idempotencia: un jugador no puede tener dos veces la misma partida.
    CONSTRAINT ux_player_matches_player_match UNIQUE (player_id, match_id)
);

CREATE INDEX ix_player_matches_player ON player_matches (player_id);
