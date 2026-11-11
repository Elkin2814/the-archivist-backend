package com.thearchivist.api.stats.infrastructure.persistence;

import com.thearchivist.api.stats.domain.models.CsrReading;
import com.thearchivist.api.stats.domain.models.MatchDetail;
import com.thearchivist.api.stats.domain.models.MatchParticipant;
import com.thearchivist.api.stats.domain.models.MmrReading;
import com.thearchivist.api.stats.domain.ports.out.MatchAnalysisRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adaptador PostgreSQL de la capa de análisis (matches / match_participants), con SQL directo.
 * Cada fila de match_participants se identifica por (partida, índice de jugador) y no guarda gamertag.
 */
@Component
public class MatchAnalysisJdbcAdapter implements MatchAnalysisRepositoryPort {

    /**
     * Columnas de match_participants (salvo la llave) con su tipo SQL y de dónde sale el valor.
     * Es la única fuente de verdad: el INSERT, el UPDATE y los parámetros se generan desde aquí.
     */
    private enum Col {
        TEAM_ID("team_id", Types.INTEGER, MatchParticipant::teamId),
        TEAM_PLAYER_INDEX("team_player_index", Types.INTEGER, MatchParticipant::teamPlayerIndex),
        LEADER_ID("leader_id", Types.INTEGER, MatchParticipant::leaderId),
        OUTCOME("outcome", Types.INTEGER, MatchParticipant::outcome),
        TEAM_OUTCOME("team_outcome", Types.INTEGER, MatchParticipant::teamOutcome),
        TEAM_SIZE("team_size", Types.INTEGER, MatchParticipant::teamSize),
        COMPLETED("completed_match", Types.BOOLEAN, MatchParticipant::completedMatch),
        HUMAN("is_human", Types.BOOLEAN, MatchParticipant::human),
        DURATION("duration_seconds", Types.BIGINT, MatchParticipant::durationSeconds),

        CSR_TIER_BEFORE("csr_tier_before", Types.INTEGER, p -> csr(p.csrBefore(), CsrReading::tier)),
        CSR_DESIGNATION_BEFORE("csr_designation_before", Types.INTEGER, p -> csr(p.csrBefore(), CsrReading::designation)),
        CSR_RAW_BEFORE("csr_raw_before", Types.INTEGER, p -> csr(p.csrBefore(), CsrReading::raw)),
        CSR_PERCENT_BEFORE("csr_percent_before", Types.INTEGER, p -> csr(p.csrBefore(), CsrReading::percentToNextTier)),
        CSR_MEASUREMENT_BEFORE("csr_measurement_remaining_before", Types.INTEGER,
                p -> csr(p.csrBefore(), CsrReading::measurementMatchesRemaining)),
        CSR_RANK_BEFORE("csr_rank_before", Types.INTEGER, p -> csr(p.csrBefore(), CsrReading::rank)),

        CSR_TIER_AFTER("csr_tier_after", Types.INTEGER, p -> csr(p.csrAfter(), CsrReading::tier)),
        CSR_DESIGNATION_AFTER("csr_designation_after", Types.INTEGER, p -> csr(p.csrAfter(), CsrReading::designation)),
        CSR_RAW_AFTER("csr_raw_after", Types.INTEGER, p -> csr(p.csrAfter(), CsrReading::raw)),
        CSR_PERCENT_AFTER("csr_percent_after", Types.INTEGER, p -> csr(p.csrAfter(), CsrReading::percentToNextTier)),
        CSR_MEASUREMENT_AFTER("csr_measurement_remaining_after", Types.INTEGER,
                p -> csr(p.csrAfter(), CsrReading::measurementMatchesRemaining)),
        CSR_RANK_AFTER("csr_rank_after", Types.INTEGER, p -> csr(p.csrAfter(), CsrReading::rank)),

        MMR_BEFORE("mmr_before", Types.DOUBLE, p -> mmr(p.mmrBefore(), MmrReading::rating)),
        MMR_VARIANCE_BEFORE("mmr_variance_before", Types.DOUBLE, p -> mmr(p.mmrBefore(), MmrReading::variance)),
        MMR_AFTER("mmr_after", Types.DOUBLE, p -> mmr(p.mmrAfter(), MmrReading::rating)),
        MMR_VARIANCE_AFTER("mmr_variance_after", Types.DOUBLE, p -> mmr(p.mmrAfter(), MmrReading::variance));

        final String column;
        final int sqlType;
        final Function<MatchParticipant, Object> value;

        Col(String column, int sqlType, Function<MatchParticipant, Object> value) {
            this.column = column;
            this.sqlType = sqlType;
            this.value = value;
        }

        private static <T> Object csr(CsrReading reading, Function<CsrReading, T> field) {
            return reading == null ? null : field.apply(reading);
        }

        private static <T> Object mmr(MmrReading reading, Function<MmrReading, T> field) {
            return reading == null ? null : field.apply(reading);
        }
    }

    /**
     * Upsert que nunca pisa un dato existente: {@code COALESCE(actual, nuevo)} solo rellena huecos
     * (por ejemplo, la fila que dejó el historial se completa con Match Result), y el WHERE evita
     * reescribir la fila cuando no hay nada que completar.
     */
    private static final String UPSERT_PARTICIPANT = buildUpsert();

    private static String buildUpsert() {
        List<Col> cols = Arrays.asList(Col.values());
        String names = cols.stream().map(c -> c.column).collect(Collectors.joining(", "));
        String values = cols.stream().map(c -> ":" + c.column).collect(Collectors.joining(", "));
        String set = cols.stream()
                .map(c -> c.column + " = COALESCE(match_participants." + c.column + ", EXCLUDED." + c.column + ")")
                .collect(Collectors.joining(",\n    "));
        String guard = cols.stream()
                .map(c -> "(match_participants." + c.column + " IS NULL AND EXCLUDED." + c.column + " IS NOT NULL)")
                .collect(Collectors.joining("\n    OR "));
        return "INSERT INTO the_archivist.match_participants (match_id, player_index, " + names + ")\n"
                + "VALUES (:match_id, :player_index, " + values + ")\n"
                + "ON CONFLICT (match_id, player_index) DO UPDATE SET\n    " + set + "\n"
                + "WHERE " + guard;
    }

    private final JdbcClient jdbc;

    public MatchAnalysisJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void saveParticipant(UUID matchId, MatchParticipant participant) {
        JdbcClient.StatementSpec spec = jdbc.sql(UPSERT_PARTICIPANT)
                .param("match_id", matchId)
                .param("player_index", participant.playerIndex());
        for (Col col : Col.values()) {
            spec = spec.param(col.column, col.value.apply(participant), col.sqlType);
        }
        spec.update();
        savePowers(matchId, participant);
    }

    /** Poderes lanzados: se insertan una sola vez (Match Result es inmutable), sin pisar lo ya guardado. */
    private void savePowers(UUID matchId, MatchParticipant participant) {
        participant.powersCast().forEach((power, times) -> jdbc.sql("""
                        INSERT INTO the_archivist.match_participant_powers (match_id, player_index, power_key, times_cast)
                        VALUES (:matchId, :playerIndex, :power, :times)
                        ON CONFLICT (match_id, player_index, power_key) DO NOTHING
                        """)
                .param("matchId", matchId)
                .param("playerIndex", participant.playerIndex())
                .param("power", power)
                .param("times", times)
                .update());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findMatchesPendingResult(int limit, int maxAttempts) {
        return jdbc.sql("""
                        SELECT match_id FROM the_archivist.matches
                        WHERE result_fetched_at IS NULL AND match_type = 3 AND result_attempts < :maxAttempts
                        ORDER BY start_date DESC
                        LIMIT :limit
                        """)
                .param("maxAttempts", maxAttempts)
                .param("limit", limit)
                .query((rs, i) -> rs.getObject("match_id", UUID.class))
                .list();
    }

    @Override
    @Transactional
    public void saveMatchDetail(MatchDetail detail) {
        if (detail.participants().isEmpty()) {
            throw new IllegalStateException("Match Result sin jugadores para la partida " + detail.matchId());
        }
        // La fila de matches ya existe (de ahí sale la cola); solo se completa, nunca se reemplaza.
        int updated = jdbc.sql("""
                        UPDATE the_archivist.matches
                        SET season_id = COALESCE(season_id, :season),
                            playlist_id = COALESCE(playlist_id, :playlist),
                            map_id = COALESCE(map_id, :map),
                            duration_seconds = :duration,
                            is_match_complete = :complete,
                            match_end_reason = :endReason,
                            victory_condition = :victory,
                            result_status = :status,
                            result_fetched_at = now()
                        WHERE match_id = :matchId
                        """)
                .param("matchId", detail.matchId())
                .param("season", detail.seasonId(), Types.OTHER)
                .param("playlist", detail.playlistId(), Types.OTHER)
                .param("map", detail.mapId(), Types.VARCHAR)
                .param("duration", detail.durationSeconds(), Types.BIGINT)
                .param("complete", detail.complete(), Types.BOOLEAN)
                .param("endReason", detail.endReason(), Types.INTEGER)
                .param("victory", detail.victoryCondition(), Types.INTEGER)
                .param("status", detail.resultStatus().name(), Types.VARCHAR)
                .update();
        if (updated == 0) {
            throw new IllegalStateException("La partida " + detail.matchId() + " no existe en matches");
        }
        for (MatchParticipant participant : detail.participants()) {
            saveParticipant(detail.matchId(), participant);
        }
    }

    @Override
    @Transactional
    public void registerFailedAttempt(UUID matchId) {
        jdbc.sql("UPDATE the_archivist.matches SET result_attempts = result_attempts + 1 WHERE match_id = :matchId")
                .param("matchId", matchId)
                .update();
    }
}
