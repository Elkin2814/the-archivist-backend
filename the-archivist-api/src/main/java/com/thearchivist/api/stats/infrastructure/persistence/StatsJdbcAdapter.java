package com.thearchivist.api.stats.infrastructure.persistence;

import com.thearchivist.api.stats.domain.enums.BreakdownDimension;
import com.thearchivist.api.stats.domain.models.BreakdownRow;
import com.thearchivist.api.stats.domain.models.LeaderRow;
import com.thearchivist.api.stats.domain.models.PairRow;
import com.thearchivist.api.stats.domain.models.StatsFilter;
import com.thearchivist.api.stats.domain.models.StatsSample;
import com.thearchivist.api.stats.domain.ports.out.StatsRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Estadísticas con SQL directo sobre {@code v_valid_participants} (matchmaking, partida válida, humanos).
 * El filtro de rango, modo y fecha se aplica al jugador que se mide (alias {@code a}); la duración mínima, a la partida.
 */
@Component
public class StatsJdbcAdapter implements StatsRepositoryPort {

    private final JdbcClient jdbc;

    public StatsJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Condiciones del filtro sobre la fila con alias {@code a}; los parámetros se enlazan con {@link #bind}. */
    static String conditions(StatsFilter f) {
        StringBuilder sb = new StringBuilder("a.match_seconds >= :minSeconds");
        if (f.playlistId() != null) sb.append(" AND a.playlist_id = :playlist");
        if (f.minDesignation() != null) sb.append(" AND a.csr_designation_before >= :minDesignation");
        if (f.maxDesignation() != null) sb.append(" AND a.csr_designation_before <= :maxDesignation");
        if (f.teamSize() != null) sb.append(" AND a.team_size = :teamSize");
        if (f.gameMode() != null) sb.append(" AND a.game_mode = :gameMode");
        if (f.from() != null) sb.append(" AND a.start_date >= :from");
        return sb.toString();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, StatsFilter f) {
        spec = spec.param("minSeconds", f.minSeconds());
        if (f.playlistId() != null) spec = spec.param("playlist", f.playlistId());
        if (f.minDesignation() != null) spec = spec.param("minDesignation", f.minDesignation());
        if (f.maxDesignation() != null) spec = spec.param("maxDesignation", f.maxDesignation());
        if (f.teamSize() != null) spec = spec.param("teamSize", f.teamSize());
        if (f.gameMode() != null) spec = spec.param("gameMode", f.gameMode());
        if (f.from() != null) spec = spec.param("from", Timestamp.from(f.from()));
        return spec;
    }

    @Override
    @Transactional(readOnly = true)
    public StatsSample sample(StatsFilter f) {
        return bind(jdbc.sql("""
                SELECT count(DISTINCT a.match_id) AS matches, count(*) AS participants,
                       min(a.start_date) AS oldest, max(a.start_date) AS newest
                FROM the_archivist.v_valid_participants a
                WHERE\s""" + conditions(f)), f)
                .query((rs, i) -> new StatsSample(
                        rs.getLong("matches"), rs.getLong("participants"),
                        instant(rs.getTimestamp("oldest")), instant(rs.getTimestamp("newest"))))
                .single();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderRow> leaderRows(StatsFilter f) {
        return bind(jdbc.sql("""
                SELECT a.leader_id, count(*) AS matches, count(*) FILTER (WHERE a.won) AS wins
                FROM the_archivist.v_valid_participants a
                WHERE\s""" + conditions(f) + " GROUP BY a.leader_id"), f)
                .query((rs, i) -> new LeaderRow(rs.getInt("leader_id"), rs.getLong("matches"), rs.getLong("wins")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PairRow> matchups(StatsFilter f, int leaderId) {
        return bind(jdbc.sql("""
                SELECT b.leader_id AS other, count(*) AS matches, count(*) FILTER (WHERE a.won) AS wins
                FROM the_archivist.v_valid_participants a
                JOIN the_archivist.v_valid_participants b ON b.match_id = a.match_id AND b.team_id <> a.team_id
                WHERE a.leader_id = :leaderId AND\s""" + conditions(f) + " GROUP BY b.leader_id"), f)
                .param("leaderId", leaderId)
                .query((rs, i) -> new PairRow(rs.getInt("other"), rs.getLong("matches"), rs.getLong("wins")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PairRow> synergies(StatsFilter f, int leaderId) {
        return bind(jdbc.sql("""
                SELECT b.leader_id AS other, count(*) AS matches, count(*) FILTER (WHERE a.won) AS wins
                FROM the_archivist.v_valid_participants a
                JOIN the_archivist.v_valid_participants b
                  ON b.match_id = a.match_id AND b.team_id = a.team_id AND b.player_index <> a.player_index
                WHERE a.leader_id = :leaderId AND\s""" + conditions(f) + " GROUP BY b.leader_id"), f)
                .param("leaderId", leaderId)
                .query((rs, i) -> new PairRow(rs.getInt("other"), rs.getLong("matches"), rs.getLong("wins")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BreakdownRow> breakdown(StatsFilter f, int leaderId, BreakdownDimension dimension) {
        // La columna sale de una lista cerrada (el enum), nunca de la petición.
        String column = "a." + dimension.column();
        return bind(jdbc.sql("SELECT " + column + "::text AS k, count(*) AS matches, count(*) FILTER (WHERE a.won) AS wins"
                + " FROM the_archivist.v_valid_participants a"
                + " WHERE a.leader_id = :leaderId AND " + column + " IS NOT NULL AND " + conditions(f)
                + " GROUP BY " + column), f)
                .param("leaderId", leaderId)
                .query((rs, i) -> new BreakdownRow(rs.getString("k"), rs.getLong("matches"), rs.getLong("wins")))
                .list();
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
