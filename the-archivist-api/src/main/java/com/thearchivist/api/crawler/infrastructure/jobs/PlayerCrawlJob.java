package com.thearchivist.api.crawler.infrastructure.jobs;

import com.thearchivist.api.crawler.domain.models.Reports;
import com.thearchivist.api.crawler.domain.ports.in.CrawlPlayersUseCase;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Recorre el historial de los jugadores de la cola en lotes acotados (jugadores y páginas por corrida), y limpia
 * a diario la cola y la clasificación con la misma retención que la caché de jugadores.
 * Todas las llamadas pasan por {@code HaloApiGate}, que mantiene el ritmo por debajo del límite de la llave.
 */
@Component
@ConditionalOnProperty(name = "archivist.crawl.enabled", havingValue = "true", matchIfMissing = true)
public class PlayerCrawlJob {

    private static final Logger log = LoggerFactory.getLogger(PlayerCrawlJob.class);

    private final CrawlPlayersUseCase crawl;
    private final CrawlQueuePort queue;
    private final LeaderboardRepositoryPort leaderboards;
    private final int maxPlayersPerRun;
    private final int maxPagesPerPlayer;
    private final Duration retention;

    public PlayerCrawlJob(CrawlPlayersUseCase crawl,
                          CrawlQueuePort queue,
                          LeaderboardRepositoryPort leaderboards,
                          @Value("${archivist.crawl.max-players-per-run:3}") int maxPlayersPerRun,
                          @Value("${archivist.crawl.max-pages-per-player:10}") int maxPagesPerPlayer,
                          @Value("${archivist.retention.player-cache-days:30}") long retentionDays) {
        this.crawl = crawl;
        this.queue = queue;
        this.leaderboards = leaderboards;
        this.maxPlayersPerRun = maxPlayersPerRun;
        this.maxPagesPerPlayer = maxPagesPerPlayer;
        this.retention = Duration.ofDays(retentionDays);
    }

    // fixedDelay: la siguiente corrida empieza N ms después de que termina la anterior (nunca se solapan).
    @Scheduled(initialDelayString = "${archivist.crawl.initial-delay-ms:150000}",
            fixedDelayString = "${archivist.crawl.fixed-delay-ms:300000}")
    public void run() {
        try {
            Reports.Crawl report = crawl.crawl(maxPlayersPerRun, maxPagesPerPlayer);
            if (report.players() > 0) {
                log.info("Rastreo de jugadores: {} jugadores, {} páginas, {} partidas nuevas, {} al día{}",
                        report.players(), report.pages(), report.newMatches(), report.completed(),
                        report.stoppedByApi() ? " (detenido por la Halo API)" : "");
            }
        } catch (RuntimeException e) {
            log.error("El rastreo de jugadores falló: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${archivist.retention.purge-cron:0 30 4 * * *}")
    public void purge() {
        Instant cutoff = Instant.now().minus(retention);
        int players = queue.purgeStale(cutoff);
        int entries = leaderboards.purgeStale(cutoff);
        log.info("Limpieza de rastreo: {} jugadores de la cola y {} puestos de clasificación anteriores a {}",
                players, entries, cutoff);
    }
}
