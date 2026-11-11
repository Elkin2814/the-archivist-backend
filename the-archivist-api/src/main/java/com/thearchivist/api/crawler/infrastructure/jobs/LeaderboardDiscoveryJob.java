package com.thearchivist.api.crawler.infrastructure.jobs;

import com.thearchivist.api.crawler.domain.models.Reports;
import com.thearchivist.api.crawler.domain.ports.in.DiscoverTopPlayersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Baja el leaderboard oficial de las colas clasificatorias y mete a los mejores jugadores en la cola de rastreo.
 * Son pocas llamadas: una por cola. Se repite como mucho cada {@code discovery-interval-ms} (un día), pero la
 * tarea se revisa cada {@code discovery-retry-ms}: si una corrida no se completó (la Halo API pidió esperar, o la
 * Metadata todavía no estaba lista) se reintenta pronto en vez de esperar un día entero.
 */
@Component
@ConditionalOnProperty(name = "archivist.crawl.enabled", havingValue = "true", matchIfMissing = true)
public class LeaderboardDiscoveryJob {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardDiscoveryJob.class);

    private final DiscoverTopPlayersUseCase discover;
    private final int topPerPlaylist;
    private final List<String> seeds;
    private final Duration interval;
    private volatile Instant lastComplete;

    public LeaderboardDiscoveryJob(DiscoverTopPlayersUseCase discover,
                                   @Value("${archivist.crawl.top-players-per-playlist:5}") int topPerPlaylist,
                                   @Value("${archivist.crawl.seed-gamertags:}") String seedGamertags,
                                   @Value("${archivist.crawl.discovery-interval-ms:86400000}") long intervalMs) {
        this.discover = discover;
        this.topPerPlaylist = topPerPlaylist;
        this.interval = Duration.ofMillis(intervalMs);
        this.seeds = seedGamertags == null || seedGamertags.isBlank() ? List.of()
                : Arrays.stream(seedGamertags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Scheduled(initialDelayString = "${archivist.crawl.discovery-initial-delay-ms:90000}",
            fixedDelayString = "${archivist.crawl.discovery-retry-ms:1800000}")
    public void run() {
        Instant last = lastComplete;
        if (last != null && last.plus(interval).isAfter(Instant.now())) return;
        try {
            Reports.Discovery report = discover.discover(topPerPlaylist, seeds);
            log.info("Descubrimiento de jugadores: {} colas, {} puestos, {} nuevos{}", report.playlists(),
                    report.entries(), report.newPlayers(), report.stoppedByApi() ? " (detenido por la Halo API)" : "");
            if (!report.stoppedByApi() && report.playlists() > 0) lastComplete = Instant.now();
        } catch (RuntimeException e) {
            log.error("El descubrimiento de jugadores falló: {}", e.getMessage(), e);
        }
    }
}
