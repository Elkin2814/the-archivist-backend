-- V5: qué partidas cuentan para las estadísticas + poderes lanzados por jugador.
--
-- Hallazgos con datos reales de la Halo API (1.090 partidas de matchmaking de una cuenta):
--   * Caída de servidor / conexión: MatchEndReason = 3, VictoryCondition = 0, los DOS equipos con
--     MatchOutcome = 0 y todos los jugadores con PlayerMatchOutcome = 2 (derrota para ambos). Dura lo normal
--     (mediana ~16 min), así que la duración no sirve para detectarlas.
--   * También hay partidas con MatchEndReason = 1 donde ambos equipos figuran en derrota.
--   * PlayerCompletedMatch = false NO es una señal de error: lo tiene quien se rinde o se va.
--   * IsMatchComplete vale true incluso en las caídas.
-- Por eso la clasificación se basa en el resultado (¿hay ganador y perdedor?) y en MatchEndReason.
-- Se calcula al ingerir Match Result (ResultStatus en el dominio) y se guarda aquí.
--
--   VALID         hay ganador y perdedor, MatchEndReason = 1 y la partida está completa (la única que cuenta)
--   NO_WINNER     nadie ganó o nadie perdió (caída de servidor, todos desconectados, doble derrota)
--   ABNORMAL_END  hay ganador pero MatchEndReason <> 1 (se excluye hasta entender ese código)
--   INCOMPLETE    IsMatchComplete = false
-- NULL = todavía no se ha pedido Match Result (result_fetched_at IS NULL).
ALTER TABLE matches ADD COLUMN result_status VARCHAR(16);

ALTER TABLE matches ADD CONSTRAINT ck_matches_result_status
    CHECK (result_status IN ('VALID', 'NO_WINNER', 'ABNORMAL_END', 'INCOMPLETE'));

-- Partidas que V4 ya hubiera ingerido: se clasifican con la misma regla que el código, a partir de los jugadores.
UPDATE matches m
SET result_status = CASE
        WHEN NOT EXISTS (SELECT 1 FROM match_participants p WHERE p.match_id = m.match_id AND p.outcome = 1)
          OR NOT EXISTS (SELECT 1 FROM match_participants p WHERE p.match_id = m.match_id AND p.outcome = 2)
            THEN 'NO_WINNER'
        WHEN m.is_match_complete IS NOT TRUE THEN 'INCOMPLETE'
        WHEN m.match_end_reason IS DISTINCT FROM 1 THEN 'ABNORMAL_END'
        ELSE 'VALID'
    END
WHERE m.result_fetched_at IS NOT NULL;

-- Las estadísticas solo miran las válidas: índice parcial por cola y temporada (reemplaza al de V4).
DROP INDEX ix_matches_playlist_season;
CREATE INDEX ix_matches_valid_playlist_season ON matches (playlist_id, season_id) WHERE result_status = 'VALID';

-- Poderes de líder que lanzó cada jugador (Match Result > LeaderPowerStats; solo trae los usados).
-- Permite medir, por ejemplo, con qué líderes o poderes se cae el servidor. Sin gamertag, como el resto de la capa 3.
CREATE TABLE match_participant_powers (
    match_id     UUID         NOT NULL,
    player_index INTEGER      NOT NULL,
    power_key    VARCHAR(100) NOT NULL,
    times_cast   INTEGER      NOT NULL,
    PRIMARY KEY (match_id, player_index, power_key),
    FOREIGN KEY (match_id, player_index) REFERENCES match_participants (match_id, player_index) ON DELETE CASCADE
);

CREATE INDEX ix_match_participant_powers_key ON match_participant_powers (power_key);

-- Filtro estándar de las estadísticas en un solo lugar: matchmaking, partida válida, jugador humano y resultado
-- 1 (victoria) o 2 (derrota). NO filtra por completed_match (quien se rinde lo tiene en false y se perderían derrotas)
-- ni por duración: la duración mínima es un parámetro de cada consulta (match_seconds >= :min).
CREATE VIEW v_valid_participants AS
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
       m.duration_seconds AS match_seconds
FROM match_participants p
         JOIN matches m ON m.match_id = p.match_id
WHERE m.result_status = 'VALID'
  AND m.match_type = 3
  AND p.is_human
  AND p.outcome IN (1, 2);
