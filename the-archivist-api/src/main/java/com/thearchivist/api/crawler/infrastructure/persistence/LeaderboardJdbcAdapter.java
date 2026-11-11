package com.thearchivist.api.crawler.infrastructure.persistence;

import com.thearchivist.api.crawler.domain.models.LeaderboardEntry;
import com.thearchivist.api.crawler.domain.models.RankedPlayer;
import com.thearchivist.api.crawler.domain.models.RankedPlaylist;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Copia local del leaderboard oficial (tabla leaderboard_entries). */
@Component
public class LeaderboardJdbcAdapter implements LeaderboardRepositoryPort {

    /** Temporada más reciente guardada para la cola {@code :playlist}. */
    private static final String LATEST_SEASON = """
            (SELECT season_id FROM the_archivist.leaderboard_entries
             WHERE playlist_id = :playlist ORDER BY fetched_at DESC LIMIT 1)""";

    private final JdbcClient jdbc;

    public LeaderboardJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void replaceEntries(UUID seasonId, UUID playlistId, List<LeaderboardEntry> entries) {
        jdbc.sql("DELETE FROM the_archivist.leaderboard_entries WHERE season_id = :season AND playlist_id = :playlist")
                .param("season", seasonId).param("playlist", playlistId)
                .update();
        for (LeaderboardEntry e : entries) {
            jdbc.sql("""
                            INSERT INTO the_archivist.leaderboard_entries
                                (season_id, playlist_id, rank, gamertag, csr_designation, csr_tier, csr, percent_to_next_tier)
                            VALUES (:season, :playlist, :rank, :gamertag, :designation, :tier, :csr, :percent)
                            """)
                    .param("season", seasonId).param("playlist", playlistId)
                    .param("rank", e.rank()).param("gamertag", e.gamertag())
                    .param("designation", e.csrDesignation(), Types.INTEGER)
                    .param("tier", e.csrTier(), Types.INTEGER)
                    .param("csr", e.csr(), Types.INTEGER)
                    .param("percent", e.percentToNextTier(), Types.INTEGER)
                    .update();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankedPlaylist> findPlaylists() {
        return jdbc.sql("""
                        SELECT e.playlist_id, COALESCE(p.name, e.playlist_id::text) AS name, e.season_id, count(*) AS players
                        FROM the_archivist.leaderboard_entries e
                        LEFT JOIN the_archivist.playlists p ON p.playlist_id = e.playlist_id
                        WHERE e.season_id = (SELECT e2.season_id FROM the_archivist.leaderboard_entries e2
                                             WHERE e2.playlist_id = e.playlist_id ORDER BY e2.fetched_at DESC LIMIT 1)
                        GROUP BY e.playlist_id, p.name, e.season_id
                        ORDER BY name
                        """)
                .query((rs, i) -> new RankedPlaylist(
                        rs.getObject("playlist_id", UUID.class), rs.getString("name"),
                        rs.getObject("season_id", UUID.class), rs.getLong("players")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankedPlayer> findPlayers(UUID playlistId, int limit, int offset) {
        return jdbc.sql("""
                        SELECT e.rank, e.gamertag, e.csr_designation, d.name AS designation_name, e.csr_tier, e.csr,
                               e.percent_to_next_tier,
                               (SELECT count(*) FROM the_archivist.player_matches pm
                                  JOIN the_archivist.players pl ON pl.id = pm.player_id
                                WHERE lower(pl.gamer_tag) = lower(e.gamertag)) AS tracked
                        FROM the_archivist.leaderboard_entries e
                        LEFT JOIN the_archivist.csr_designations d ON d.designation_id = e.csr_designation
                        WHERE e.playlist_id = :playlist AND e.season_id = """ + LATEST_SEASON + """

                        ORDER BY e.rank
                        LIMIT :limit OFFSET :offset
                        """)
                .param("playlist", playlistId).param("limit", limit).param("offset", offset)
                .query((rs, i) -> new RankedPlayer(
                        rs.getInt("rank"), rs.getString("gamertag"),
                        (Integer) rs.getObject("csr_designation"), rs.getString("designation_name"),
                        (Integer) rs.getObject("csr_tier"), (Integer) rs.getObject("csr"),
                        (Integer) rs.getObject("percent_to_next_tier"), rs.getLong("tracked")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPlayers(UUID playlistId) {
        return jdbc.sql("""
                        SELECT count(*) FROM the_archivist.leaderboard_entries
                        WHERE playlist_id = :playlist AND season_id = """ + LATEST_SEASON)
                .param("playlist", playlistId)
                .query(Long.class)
                .single();
    }

    @Override
    @Transactional
    public int purgeStale(Instant cutoff) {
        return jdbc.sql("DELETE FROM the_archivist.leaderboard_entries WHERE fetched_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }
}
