package com.thearchivist.api.crawler.infrastructure.persistence;

import com.thearchivist.api.crawler.domain.enums.CrawlStatus;
import com.thearchivist.api.crawler.domain.models.CrawlPlayer;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Cola de rastreo en PostgreSQL (tabla crawl_players). */
@Component
public class CrawlQueueJdbcAdapter implements CrawlQueuePort {

    private final JdbcClient jdbc;

    public CrawlQueueJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public int enqueue(List<Seed> seeds) {
        int inserted = 0;
        for (Seed seed : seeds) {
            // xmax = 0 solo en las filas recién insertadas: distingue "nuevo" de "ya estaba".
            Boolean isNew = jdbc.sql("""
                            INSERT INTO the_archivist.crawl_players (gamertag, source, priority)
                            VALUES (:gamertag, :source, :priority)
                            ON CONFLICT (lower(gamertag)) DO UPDATE
                                SET priority = GREATEST(crawl_players.priority, EXCLUDED.priority),
                                    updated_at = now()
                            RETURNING (xmax = 0)
                            """)
                    .param("gamertag", seed.gamertag())
                    .param("source", seed.source().name())
                    .param("priority", seed.priority())
                    .query(Boolean.class)
                    .single();
            if (Boolean.TRUE.equals(isNew)) inserted++;
        }
        return inserted;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CrawlPlayer> claimNext(Instant now) {
        return jdbc.sql("""
                        SELECT id, gamertag, status, history_cursor, history_complete, attempts
                        FROM the_archivist.crawl_players
                        WHERE status = 'PENDING' OR (status = 'DONE' AND next_crawl_at <= :now)
                        ORDER BY priority DESC, last_crawled_at NULLS FIRST, id
                        LIMIT 1
                        """)
                .param("now", Timestamp.from(now))
                .query((rs, i) -> new CrawlPlayer(
                        rs.getLong("id"),
                        rs.getString("gamertag"),
                        CrawlStatus.valueOf(rs.getString("status")),
                        rs.getInt("history_cursor"),
                        rs.getBoolean("history_complete"),
                        rs.getInt("attempts")))
                .optional();
    }

    @Override
    @Transactional
    public void saveProgress(long id, int cursor, int newMatches) {
        jdbc.sql("""
                        UPDATE the_archivist.crawl_players
                        SET status = 'PENDING', history_cursor = :cursor, matches_saved = matches_saved + :added,
                            attempts = 0, last_error = NULL, last_crawled_at = now(), updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", id).param("cursor", cursor).param("added", newMatches)
                .update();
    }

    @Override
    @Transactional
    public void markCompleted(long id, int newMatches, Instant nextCrawlAt) {
        jdbc.sql("""
                        UPDATE the_archivist.crawl_players
                        SET status = 'DONE', history_complete = TRUE, history_cursor = 0,
                            matches_saved = matches_saved + :added, attempts = 0, last_error = NULL,
                            last_crawled_at = now(), next_crawl_at = :next, updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", id).param("added", newMatches).param("next", Timestamp.from(nextCrawlAt))
                .update();
    }

    @Override
    @Transactional
    public void markNotFound(long id, String message) {
        jdbc.sql("""
                        UPDATE the_archivist.crawl_players
                        SET status = 'NOT_FOUND', last_error = :message, last_crawled_at = now(), updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", id).param("message", message, Types.VARCHAR)
                .update();
    }

    @Override
    @Transactional
    public void postpone(long id, String message) {
        jdbc.sql("""
                        UPDATE the_archivist.crawl_players
                        SET last_error = :message, last_crawled_at = now(), updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", id).param("message", message, Types.VARCHAR)
                .update();
    }

    @Override
    @Transactional
    public void markFailed(long id, String message, int maxAttempts) {
        jdbc.sql("""
                        UPDATE the_archivist.crawl_players
                        SET attempts = attempts + 1,
                            status = CASE WHEN attempts + 1 >= :max THEN 'FAILED' ELSE status END,
                            last_error = :message, last_crawled_at = now(), updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", id).param("max", maxAttempts).param("message", message, Types.VARCHAR)
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CrawlStatus, Long> countByStatus() {
        Map<CrawlStatus, Long> result = new EnumMap<>(CrawlStatus.class);
        jdbc.sql("SELECT status, count(*) AS n FROM the_archivist.crawl_players GROUP BY status")
                .query((rs, i) -> {
                    result.put(CrawlStatus.valueOf(rs.getString("status")), rs.getLong("n"));
                    return null;
                })
                .list();
        return result;
    }

    @Override
    @Transactional
    public int purgeStale(Instant cutoff) {
        return jdbc.sql("DELETE FROM the_archivist.crawl_players WHERE updated_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }
}
