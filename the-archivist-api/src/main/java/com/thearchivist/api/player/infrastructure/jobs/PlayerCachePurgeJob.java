package com.thearchivist.api.player.infrastructure.jobs;

import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Limpia la caché de historiales: borra a los jugadores que nadie consulta hace
 * {@code archivist.retention.player-cache-days} días, junto con su lista de partidas
 * (player_matches). Los datos de análisis (matches y match_participants) no se tocan.
 */
@Component
public class PlayerCachePurgeJob {

    private static final Logger log = LoggerFactory.getLogger(PlayerCachePurgeJob.class);

    private final PlayerRepositoryPort playerRepositoryPort;
    private final Duration retention;

    public PlayerCachePurgeJob(PlayerRepositoryPort playerRepositoryPort,
                               @Value("${archivist.retention.player-cache-days:30}") long days) {
        this.playerRepositoryPort = playerRepositoryPort;
        this.retention = Duration.ofDays(days);
    }

    @Scheduled(cron = "${archivist.retention.purge-cron:0 30 4 * * *}")
    public void purge() {
        Instant cutoff = Instant.now().minus(retention);
        int deleted = playerRepositoryPort.deleteInactivePlayers(cutoff);
        log.info("Limpieza de caché: {} jugadores sin consultas desde {} eliminados", deleted, cutoff);
    }
}
