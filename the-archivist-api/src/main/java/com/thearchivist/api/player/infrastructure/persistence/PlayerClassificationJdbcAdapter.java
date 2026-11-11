package com.thearchivist.api.player.infrastructure.persistence;

import com.thearchivist.api.player.domain.models.PlayerClassification.Standing;
import com.thearchivist.api.player.domain.ports.out.PlayerClassificationPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lecturas de la clasificación de un jugador con SQL directo. El rango sale de la última partida con CSR/MMR de
 * cada cola (valor exacto "después" de esa partida); las caídas de servidor ya detectadas no cuentan como derrota.
 */
@Component
public class PlayerClassificationJdbcAdapter implements PlayerClassificationPort {

    private final JdbcClient jdbc;

    public PlayerClassificationJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaylistRecord> findPlaylistRecords(String gamertag) {
        return jdbc.sql("""
                        WITH mine AS (
                            SELECT m.playlist_id, m.start_date, pm.outcome,
                                   pm.csr_designation_after, pm.csr_tier_after, pm.csr_raw_after, pm.csr_percent_after,
                                   pm.csr_measurement_remaining_after, pm.mmr_after, pm.mmr_variance_after
                            FROM the_archivist.player_matches pm
                                 JOIN the_archivist.players pl ON pl.id = pm.player_id
                                 JOIN the_archivist.matches m ON m.match_id = pm.match_id
                            WHERE lower(pl.gamer_tag) = lower(:tag)
                              AND m.match_type = 3
                              AND m.playlist_id IS NOT NULL
                              AND (m.result_status IS NULL OR m.result_status = 'VALID')
                        ), agg AS (
                            SELECT playlist_id,
                                   count(*) FILTER (WHERE outcome IN (1, 2)) AS matches,
                                   count(*) FILTER (WHERE outcome = 1)       AS wins,
                                   max(start_date)                           AS last_match
                            FROM mine GROUP BY playlist_id
                        ), last_rated AS (
                            SELECT DISTINCT ON (playlist_id) *
                            FROM mine
                            WHERE csr_designation_after IS NOT NULL OR mmr_after IS NOT NULL
                            ORDER BY playlist_id, start_date DESC
                        )
                        SELECT a.playlist_id, COALESCE(p.name, a.playlist_id::text) AS name,
                               a.matches, a.wins, a.last_match,
                               (r.playlist_id IS NOT NULL) AS rated,
                               r.csr_designation_after, d.name AS designation_name, r.csr_tier_after,
                               NULLIF(r.csr_raw_after, 0) AS csr, r.csr_percent_after,
                               r.csr_measurement_remaining_after, r.mmr_after, r.mmr_variance_after
                        FROM agg a
                             LEFT JOIN last_rated r ON r.playlist_id = a.playlist_id
                             LEFT JOIN the_archivist.playlists p ON p.playlist_id = a.playlist_id
                             LEFT JOIN the_archivist.csr_designations d ON d.designation_id = r.csr_designation_after
                        WHERE a.matches > 0
                        ORDER BY a.matches DESC, a.playlist_id
                        """)
                .param("tag", gamertag)
                .query((rs, i) -> new PlaylistRecord(
                        rs.getObject("playlist_id", UUID.class), rs.getString("name"),
                        rs.getLong("matches"), rs.getLong("wins"), instant(rs.getTimestamp("last_match")),
                        rs.getBoolean("rated") ? standing(rs) : null))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> findLeaderboardRank(String gamertag, UUID playlistId) {
        return jdbc.sql("""
                        SELECT e.rank FROM the_archivist.leaderboard_entries e
                        WHERE e.playlist_id = :playlist AND lower(e.gamertag) = lower(:tag)
                          AND e.season_id = (SELECT e2.season_id FROM the_archivist.leaderboard_entries e2
                                             WHERE e2.playlist_id = :playlist ORDER BY e2.fetched_at DESC LIMIT 1)
                        """)
                .param("playlist", playlistId).param("tag", gamertag)
                .query(Integer.class)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Percentile> findMmrPercentile(UUID playlistId, double mmr, Instant since) {
        return jdbc.sql("""
                        SELECT count(*) AS sample, count(*) FILTER (WHERE mmr_before < :mmr) AS below
                        FROM the_archivist.v_valid_participants
                        WHERE playlist_id = :playlist AND mmr_before IS NOT NULL AND start_date >= :since
                        """)
                .param("playlist", playlistId).param("mmr", mmr).param("since", Timestamp.from(since))
                .query((rs, i) -> {
                    long sample = rs.getLong("sample");
                    return sample == 0 ? null : new Percentile(100.0 * rs.getLong("below") / sample, sample);
                })
                .optional();
    }

    private static Standing standing(ResultSet rs) throws SQLException {
        return new Standing(
                intOrNull(rs, "csr_designation_after"), rs.getString("designation_name"),
                intOrNull(rs, "csr_tier_after"), intOrNull(rs, "csr"), intOrNull(rs, "csr_percent_after"),
                intOrNull(rs, "csr_measurement_remaining_after"),
                doubleOrNull(rs, "mmr_after"), doubleOrNull(rs, "mmr_variance_after"), null, 0);
    }

    private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    private static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
