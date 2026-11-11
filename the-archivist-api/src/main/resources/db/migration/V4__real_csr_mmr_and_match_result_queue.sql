-- V4: CSR y MMR reales (antes y después de cada partida) + cola de ingesta de "Match Result".
--
-- Tanto "Player Match History" como "Match Result" entregan, por jugador, un bloque RatingProgress:
--   PreviousCsr / UpdatedCsr : Tier, Designation, Raw, PercentToNextTier, MeasurementMatchesRemaining, Rank
--   PreviousMmr / UpdatedMmr : Rating, Variance
-- Es el valor exacto de ese momento, así que no hace falta aproximarlo con una "foto" del rango actual.
--
-- Designation (ID de la Metadata de csr-designations):
--   0 Unranked, 1 Bronze, 2 Silver, 3 Gold, 4 Platinum, 5 Diamond, 6 Onyx, 7 Champion
-- Tier sube mientras se avanza dentro de la designación y PercentToNextTier es el avance hacia el siguiente tier.
-- Raw y Rank solo tienen sentido en los rangos más altos (en el resto llegan 0 / null).
--
-- Todas las columnas nuevas admiten NULL: las colas no clasificatorias no traen CSR.

-- Capa 1: código legible de la cola (HW2Playlist.Id, por ejemplo CP_RANKED_3v3_WAR).
ALTER TABLE playlists ADD COLUMN code VARCHAR(64);

-- Capa 2 (caché del jugador): se agrega el detalle completo para poder mostrar el progreso por partida.
-- Ya existían csr_tier_*, csr_designation_*, mmr_before y mmr_after.
ALTER TABLE player_matches
    ADD COLUMN csr_raw_before                   INTEGER,
    ADD COLUMN csr_percent_before               INTEGER,
    ADD COLUMN csr_measurement_remaining_before INTEGER,
    ADD COLUMN csr_rank_before                  INTEGER,
    ADD COLUMN csr_raw_after                    INTEGER,
    ADD COLUMN csr_percent_after                INTEGER,
    ADD COLUMN csr_measurement_remaining_after  INTEGER,
    ADD COLUMN csr_rank_after                   INTEGER,
    ADD COLUMN mmr_variance_before              DOUBLE PRECISION,
    ADD COLUMN mmr_variance_after               DOUBLE PRECISION;

-- Capa 3 (análisis, sin gamertag): una fila por jugador-en-partida, con el mismo detalle.
-- Ya existían csr_tier_before, csr_designation_before y mmr_before.
ALTER TABLE match_participants
    ADD COLUMN team_player_index                INTEGER,
    ADD COLUMN is_human                         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN csr_raw_before                   INTEGER,
    ADD COLUMN csr_percent_before               INTEGER,
    ADD COLUMN csr_measurement_remaining_before INTEGER,
    ADD COLUMN csr_rank_before                  INTEGER,
    ADD COLUMN csr_tier_after                   INTEGER,
    ADD COLUMN csr_designation_after            INTEGER,
    ADD COLUMN csr_raw_after                    INTEGER,
    ADD COLUMN csr_percent_after                INTEGER,
    ADD COLUMN csr_measurement_remaining_after  INTEGER,
    ADD COLUMN csr_rank_after                   INTEGER,
    ADD COLUMN mmr_variance_before              DOUBLE PRECISION,
    ADD COLUMN mmr_after                        DOUBLE PRECISION,
    ADD COLUMN mmr_variance_after               DOUBLE PRECISION;

-- Hechos a nivel de partida que solo trae "Match Result".
-- result_fetched_at es la cola de ingesta: NULL = falta llamar a Match Result para esta partida.
-- result_attempts cuenta los intentos fallidos "definitivos" (por ejemplo 404) para no reintentar sin fin.
ALTER TABLE matches
    ADD COLUMN duration_seconds    BIGINT,
    ADD COLUMN is_match_complete   BOOLEAN,
    ADD COLUMN match_end_reason    INTEGER,
    ADD COLUMN victory_condition   INTEGER,
    ADD COLUMN result_fetched_at   TIMESTAMPTZ,
    ADD COLUMN result_attempts     INTEGER NOT NULL DEFAULT 0;

-- Pendientes de ingesta (solo matchmaking), las más recientes primero.
CREATE INDEX ix_matches_result_pending ON matches (start_date DESC)
    WHERE result_fetched_at IS NULL AND match_type = 3;

-- Filtros de las estadísticas: cola y temporada.
CREATE INDEX ix_matches_playlist_season ON matches (playlist_id, season_id);

-- Winrate / popularidad por líder y rango (designación al empezar la partida).
-- Reemplaza al índice solo por líder de V2: el nuevo también lo cubre por su primera columna.
DROP INDEX ix_match_participants_leader;
CREATE INDEX ix_match_participants_leader_band
    ON match_participants (leader_id, csr_designation_before) INCLUDE (outcome, completed_match);
