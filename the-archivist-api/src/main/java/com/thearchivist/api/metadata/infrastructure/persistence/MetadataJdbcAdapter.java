package com.thearchivist.api.metadata.infrastructure.persistence;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;
import com.thearchivist.api.metadata.domain.ports.out.MetadataRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida hacia PostgreSQL para la Metadata. Son tablas de referencia sin lógica,
 * por eso se usa SQL directo (upsert) en lugar de entidades JPA.
 */
@Component
public class MetadataJdbcAdapter implements MetadataRepositoryPort {

    private final JdbcClient jdbc;

    public MetadataJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void saveMaps(List<MapInfo> maps) {
        for (MapInfo m : maps) {
            jdbc.sql("""
                    INSERT INTO the_archivist.maps (map_id, identity, name, synced_at)
                    VALUES (:id, :identity, :name, now())
                    ON CONFLICT (map_id) DO UPDATE
                    SET identity = EXCLUDED.identity, name = EXCLUDED.name, synced_at = now()
                    """)
                    .param("id", m.id())
                    .param("identity", m.identity())
                    .param("name", m.name())
                    .update();
        }
    }

    @Override
    @Transactional
    public void savePlaylists(List<PlaylistInfo> playlists) {
        for (PlaylistInfo p : playlists) {
            jdbc.sql("""
                    INSERT INTO the_archivist.playlists
                        (playlist_id, name, stats_classification, team_game, hidden, synced_at)
                    VALUES (:id, :name, :stats, :team, :hidden, now())
                    ON CONFLICT (playlist_id) DO UPDATE
                    SET name = EXCLUDED.name, stats_classification = EXCLUDED.stats_classification,
                        team_game = EXCLUDED.team_game, hidden = EXCLUDED.hidden, synced_at = now()
                    """)
                    .param("id", p.id())
                    .param("name", p.name())
                    .param("stats", p.statsClassification())
                    .param("team", p.teamGame())
                    .param("hidden", p.hidden())
                    .update();
        }
    }

    @Override
    @Transactional
    public void saveSeasons(List<SeasonInfo> seasons) {
        for (SeasonInfo s : seasons) {
            jdbc.sql("""
                    INSERT INTO the_archivist.seasons (season_id, name, start_date, synced_at)
                    VALUES (:id, :name, :start, now())
                    ON CONFLICT (season_id) DO UPDATE
                    SET name = EXCLUDED.name, start_date = EXCLUDED.start_date, synced_at = now()
                    """)
                    .param("id", s.id())
                    .param("name", s.name())
                    .param("start", s.startDate() == null ? null : Timestamp.from(s.startDate()))
                    .update();
        }
    }

    @Override
    @Transactional
    public void saveCsrDesignations(List<CsrDesignationInfo> designations) {
        for (CsrDesignationInfo d : designations) {
            jdbc.sql("""
                    INSERT INTO the_archivist.csr_designations (designation_id, name, synced_at)
                    VALUES (:id, :name, now())
                    ON CONFLICT (designation_id) DO UPDATE
                    SET name = EXCLUDED.name, synced_at = now()
                    """)
                    .param("id", d.id())
                    .param("name", d.name())
                    .update();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapInfo> findMaps() {
        return jdbc.sql("SELECT map_id, identity, name FROM the_archivist.maps ORDER BY name")
                .query((rs, i) -> new MapInfo(
                        rs.getString("map_id"), rs.getObject("identity", UUID.class), rs.getString("name")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaylistInfo> findPlaylists() {
        return jdbc.sql("""
                        SELECT playlist_id, name, stats_classification, team_game, hidden
                        FROM the_archivist.playlists ORDER BY name
                        """)
                .query((rs, i) -> new PlaylistInfo(
                        rs.getObject("playlist_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("stats_classification"),
                        (Boolean) rs.getObject("team_game"),
                        (Boolean) rs.getObject("hidden")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonInfo> findSeasons() {
        return jdbc.sql("SELECT season_id, name, start_date FROM the_archivist.seasons ORDER BY start_date")
                .query((rs, i) -> {
                    Timestamp start = rs.getTimestamp("start_date");
                    return new SeasonInfo(rs.getObject("season_id", UUID.class), rs.getString("name"),
                            start == null ? null : start.toInstant());
                })
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CsrDesignationInfo> findCsrDesignations() {
        return jdbc.sql("SELECT designation_id, name FROM the_archivist.csr_designations ORDER BY designation_id")
                .query((rs, i) -> new CsrDesignationInfo(rs.getInt("designation_id"), rs.getString("name")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> oldestSync() {
        // Si alguna tabla está vacía, su min() es nulo y se considera que falta sincronizar.
        return jdbc.sql("""
                        SELECT CASE WHEN (SELECT count(*) FROM the_archivist.maps) = 0
                                      OR (SELECT count(*) FROM the_archivist.playlists) = 0
                                      OR (SELECT count(*) FROM the_archivist.seasons) = 0
                                      OR (SELECT count(*) FROM the_archivist.csr_designations) = 0
                                    THEN NULL
                                    ELSE least(
                                        (SELECT min(synced_at) FROM the_archivist.maps),
                                        (SELECT min(synced_at) FROM the_archivist.playlists),
                                        (SELECT min(synced_at) FROM the_archivist.seasons),
                                        (SELECT min(synced_at) FROM the_archivist.csr_designations))
                               END AS oldest
                        """)
                .query((rs, i) -> {
                    Timestamp ts = rs.getTimestamp("oldest");
                    return ts == null ? null : ts.toInstant();
                })
                .optional();
    }
}
