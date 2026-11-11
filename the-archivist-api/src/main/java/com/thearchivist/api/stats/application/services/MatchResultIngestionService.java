package com.thearchivist.api.stats.application.services;

import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.stats.domain.exceptions.MatchResultNotFoundException;
import com.thearchivist.api.stats.domain.models.MatchDetail;
import com.thearchivist.api.stats.domain.ports.in.IngestMatchResultsUseCase;
import com.thearchivist.api.stats.domain.ports.out.MatchAnalysisRepositoryPort;
import com.thearchivist.api.stats.domain.ports.out.MatchResultClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Una llamada a Match Result por partida pendiente: guarda líder, resultado y CSR/MMR exactos de
 * TODOS los jugadores, sin gamertag. Las partidas son inmutables, así que cada una se pide una sola vez.
 */
@Service
public class MatchResultIngestionService implements IngestMatchResultsUseCase {

    private static final Logger log = LoggerFactory.getLogger(MatchResultIngestionService.class);

    private final MatchAnalysisRepositoryPort repository;
    private final MatchResultClientPort client;
    private final long delayBetweenCallsMs;
    private final int maxAttempts;
    private final long inProgressGraceMinutes;

    public MatchResultIngestionService(MatchAnalysisRepositoryPort repository,
                                       MatchResultClientPort client,
                                       @Value("${archivist.ingestion.delay-between-calls-ms:1500}") long delayBetweenCallsMs,
                                       @Value("${archivist.ingestion.max-attempts:3}") int maxAttempts,
                                       @Value("${archivist.ingestion.in-progress-grace-minutes:180}") long inProgressGraceMinutes) {
        this.repository = repository;
        this.client = client;
        this.delayBetweenCallsMs = delayBetweenCallsMs;
        this.maxAttempts = maxAttempts;
        this.inProgressGraceMinutes = inProgressGraceMinutes;
    }

    @Override
    public Report ingestPending(int maxMatches) {
        List<UUID> pending = repository.findMatchesPendingResult(maxMatches, maxAttempts);
        int attempted = 0;
        int saved = 0;
        int notFound = 0;
        int failed = 0;
        boolean stoppedByApi = false;

        for (UUID matchId : pending) {
            if (attempted > 0) {
                pause();
            }
            attempted++;
            try {
                MatchDetail detail = client.fetchMatchResult(matchId);
                if (isStillInProgress(detail)) {
                    // La Halo API devuelve partidas en curso con datos parciales (IsMatchComplete = false) y
                    // se piden una sola vez: guardarla ahora la dejaría INCOMPLETE para siempre. Se deja
                    // pendiente y se reintenta en la próxima corrida, sin gastar intentos.
                    log.debug("Partida {} aún en curso: se reintenta más tarde", matchId);
                    continue;
                }
                repository.saveMatchDetail(detail);
                saved++;
            } catch (MatchResultNotFoundException e) {
                repository.registerFailedAttempt(matchId);
                notFound++;
                log.warn("{}", e.getMessage());
            } catch (HaloApiUnavailableException e) {
                // Límite de llamadas, llave inválida o caída: se corta y se reintenta en la próxima corrida.
                stoppedByApi = true;
                log.warn("Ingesta detenida: {}", e.getMessage());
                break;
            } catch (RuntimeException e) {
                // Una partida con datos que no se pueden guardar no debe bloquear a las demás:
                // se anota el intento y tras max-attempts deja de reintentarse.
                repository.registerFailedAttempt(matchId);
                failed++;
                log.error("No se pudo guardar la partida {}: {}", matchId, e.getMessage());
            }
        }
        return new Report(attempted, saved, notFound, failed, stoppedByApi);
    }

    /** Sin terminar y empezada hace menos de {@code inProgressGraceMinutes}: puede seguir jugándose. */
    private boolean isStillInProgress(MatchDetail detail) {
        if (Boolean.TRUE.equals(detail.complete())) return false;
        Instant start = detail.startDate();
        return start != null && start.isAfter(Instant.now().minus(inProgressGraceMinutes, ChronoUnit.MINUTES));
    }

    private void pause() {
        if (delayBetweenCallsMs <= 0) return;
        try {
            Thread.sleep(delayBetweenCallsMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
