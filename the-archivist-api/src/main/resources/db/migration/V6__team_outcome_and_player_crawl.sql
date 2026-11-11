-- V6: resultado del EQUIPO por jugador + cola temporal de jugadores a rastrear.
--
-- 1) Resultado del equipo. La documentación de la Halo API avisa de que el historial marca derrota a quien
--    abandona la partida ("If the player did not finish the match, this will indicate a loss") aunque su equipo
--    gane. Para medir líderes se cuenta la victoria del EQUIPO, que solo trae "Match Result" (Teams > MatchOutcome).
--    La columna admite NULL: las filas que dejó el historial se completan cuando se pide Match Result.
--    Match Result también trae TeamSize (1, 2 o 3 según el modo; es el tamaño NOMINAL: no baja si alguien no llegó a
--    cargar), que sirve para separar 1v1, 2v2 y 3v3.
ALTER TABLE match_participants
    ADD COLUMN team_outcome INTEGER,
    ADD COLUMN team_size    INTEGER;

-- Las partidas ya ingeridas no tienen el resultado del equipo: se vuelven a pedir una vez (el upsert solo rellena
-- huecos y recalcula result_status con la regla nueva). Mientras tanto conservan su result_status anterior.
UPDATE matches SET result_fetched_at = NULL, result_attempts = 0
WHERE result_fetched_at IS NOT NULL AND match_type = 3;

-- Filtro estándar de las estadísticas. Se añaden al final (CREATE OR REPLACE VIEW no permite reordenar):
--   team_outcome, completed_match, won (victoria del equipo; si no hay resultado de equipo, el del jugador),
--   game_mode, team_size (1v1, 2v2, 3v3), mmr_after, csr_designation_after.
CREATE OR REPLACE VIEW v_valid_participants AS
SELECT p.match_id,
       p.player_index,
       p.team_id,
       p.leader_id,
       p.outcome,
       p.csr_designation_before,
       p.csr_tier_before,
       p.csr_raw_before,
       p.mmr_before,
       p.duration_seconds AS player_seconds,
       m.playlist_id,
       m.season_id,
       m.map_id,
       m.start_date,
       m.duration_seconds AS match_seconds,
       p.team_outcome,
       p.completed_match,
       (COALESCE(p.team_outcome, p.outcome) = 1) AS won,
       m.game_mode,
       p.team_size,
       p.mmr_after,
       p.csr_designation_after
FROM match_participants p
         JOIN matches m ON m.match_id = p.match_id
WHERE m.result_status = 'VALID'
  AND m.match_type = 3
  AND p.is_human
  AND COALESCE(p.team_outcome, p.outcome) IN (1, 2);

-- 2) Clasificación de jugadores (capa 2, temporal: lleva gamertag y se purga con la misma retención que la caché
--    de jugadores). Es una copia del leaderboard oficial de la Halo API (solo trae el top 250 con CSR >= 1800 por
--    cola y temporada); cada descubrimiento reemplaza por completo la lista de su cola.
CREATE TABLE leaderboard_entries (
    season_id            UUID         NOT NULL,
    playlist_id          UUID         NOT NULL,
    rank                 INTEGER      NOT NULL,
    gamertag             VARCHAR(64)  NOT NULL,
    csr_designation      INTEGER,
    csr_tier             INTEGER,
    csr                  INTEGER,
    percent_to_next_tier INTEGER,
    fetched_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (season_id, playlist_id, rank)
);

CREATE INDEX ix_leaderboard_entries_gamertag ON leaderboard_entries (lower(gamertag));

-- 3) Cola temporal de jugadores a rastrear (capa 2, mismo motivo: lleva gamertag).
--    Se alimenta con el leaderboard o a mano, y guarda el avance del recorrido del historial.
--      history_cursor    posición (start) desde la que continuar el recorrido; 0 si no se ha empezado
--      history_complete  true cuando ya se llegó a la partida más antigua; después solo se actualiza lo nuevo
--      status            PENDING (por recorrer), DONE (al día hasta next_crawl_at),
--                        NOT_FOUND (la API no lo conoce), FAILED (agotó intentos)
--      priority          mayor primero; el leaderboard da más prioridad a los mejores puestos
CREATE TABLE crawl_players (
    id                 BIGSERIAL PRIMARY KEY,
    gamertag           VARCHAR(64)  NOT NULL,
    source             VARCHAR(16)  NOT NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    priority           INTEGER      NOT NULL DEFAULT 0,
    history_cursor     INTEGER      NOT NULL DEFAULT 0,
    history_complete   BOOLEAN      NOT NULL DEFAULT FALSE,
    matches_saved      INTEGER      NOT NULL DEFAULT 0,
    attempts           INTEGER      NOT NULL DEFAULT 0,
    last_error         VARCHAR(255),
    last_crawled_at    TIMESTAMPTZ,
    next_crawl_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    discovered_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_crawl_players_status CHECK (status IN ('PENDING', 'DONE', 'NOT_FOUND', 'FAILED')),
    CONSTRAINT ck_crawl_players_source CHECK (source IN ('LEADERBOARD', 'MANUAL'))
);

CREATE UNIQUE INDEX ux_crawl_players_gamertag_lower ON crawl_players (lower(gamertag));

-- Siguiente jugador a recorrer: mayor prioridad primero y, a igualdad, el que lleva más tiempo sin tocarse
-- (así los jugadores a medio recorrer se turnan en vez de monopolizar la cola).
CREATE INDEX ix_crawl_players_next ON crawl_players (priority DESC, last_crawled_at NULLS FIRST, id)
    WHERE status IN ('PENDING', 'DONE');
