package com.thearchivist.api.crawler.infrastructure.persistence;

import com.thearchivist.api.crawler.domain.models.TrackedPlayer;
import com.thearchivist.api.crawler.domain.ports.out.TrackedRankingPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Clasificación propia: última partida con CSR de cada jugador en la cola, más su rendimiento en ella. */
@Component
public class TrackedRankingJdbcAdapter implements TrackedRankingPort {

    /** Un jugador por fila: rango tras su última partida con CSR y MMR + partidas decididas de la cola. */
    private static final String BASE = """
            WITH rated AS (
                SELECT DISTINCT ON (pm.player_id)
                       pm.player_id, pm.csr_designation_after AS designation, pm.csr_tier_after AS tier,
                       NULLIF(pm.csr_raw_after, 0) AS csr, pm.csr_percent_after AS percent, pm.mmr_after AS mmr,
                       m.start_date
                FROM the_archivist.player_matches pm
                     JOIN the_archivist.matches m ON m.match_id = pm.match_id
                WHERE m.playlist_id = :playlist AND m.match_type = 3
                  AND pm.csr_designation_after IS NOT NULL AND pm.mmr_after IS NOT NULL
                ORDER BY pm.player_id, m.start_date DESC
            ), agg AS (
                SELECT pm.player_id,
                       count(*) FILTER (WHERE pm.outcome IN (1, 2)) AS matches,
                       count(*) FILTER (WHERE pm.outcome = 1)       AS wins
                FROM the_archivist.player_matches pm
                     JOIN the_archivist.matches m ON m.match_id = pm.match_id
                WHERE m.playlist_id = :playlist AND m.match_type = 3
                  AND (m.result_status IS NULL OR m.result_status = 'VALID')
                GROUP BY pm.player_id
            ), eligible AS (
                SELECT r.*, a.matches, a.wins
                FROM rated r JOIN agg a ON a.player_id = r.player_id
                WHERE r.start_date >= :since AND a.matches >= :minMatches
            )
            """;

    private final JdbcClient jdbc;

    public TrackedRankingJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedPlayer> findTracked(UUID playlistId, int minMatches, Instant since, int limit, int offset) {
        return jdbc.sql(BASE + """
                        SELECT * FROM (
                            SELECT row_number() OVER (ORDER BY e.designation DESC, COALESCE(e.csr, 0) DESC,
                                                               e.mmr DESC, e.player_id) AS position,
                                   pl.gamer_tag, e.designation, d.name AS designation_name, e.tier, e.csr, e.percent,
                                   e.mmr, e.matches, e.wins, e.start_date
                            FROM eligible e
                                 JOIN the_archivist.players pl ON pl.id = e.player_id
                                 LEFT JOIN the_archivist.csr_designations d ON d.designation_id = e.designation
                        ) ranked
                        ORDER BY position
                        LIMIT :limit OFFSET :offset
                        """)
                .param("playlist", playlistId).param("minMatches", minMatches)
                .param("since", Timestamp.from(since)).param("limit", limit).param("offset", offset)
                .query((rs, i) -> new TrackedPlayer(
                        rs.getInt("position"), rs.getString("gamer_tag"),
                        intOrNull(rs, "designation"), rs.getString("designation_name"),
                        intOrNull(rs, "tier"), intOrNull(rs, "csr"), intOrNull(rs, "percent"),
                        rs.getDouble("mmr"), rs.getLong("matches"), rs.getLong("wins"),
                        rs.getTimestamp("start_date").toInstant()))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTracked(UUID playlistId, int minMatches, Instant since) {
        return jdbc.sql(BASE + "SELECT count(*) FROM eligible")
                .param("playlist", playlistId).param("minMatches", minMatches).param("since", Timestamp.from(since))
                .query(Long.class)
                .single();
    }

    private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }
}
