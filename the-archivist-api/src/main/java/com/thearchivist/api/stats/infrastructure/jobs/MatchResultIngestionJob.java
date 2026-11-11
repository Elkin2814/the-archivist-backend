package com.thearchivist.api.stats.infrastructure.jobs;

import com.thearchivist.api.stats.domain.ports.in.IngestMatchResultsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que llena la capa de análisis: cada corrida pide Match Result de un lote acotado de
 * partidas pendientes (las más recientes primero) y se detiene sola si la Halo API se niega a responder.
 * El tope por corrida y la pausa entre llamadas (archivist.ingestion.*) protegen la cuota de la llave,
 * que comparte con las consultas de jugadores.
 */
@Component
@ConditionalOnProperty(name = "archivist.ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class MatchResultIngestionJob {

    private static final Logger log = LoggerFactory.getLogger(MatchResultIngestionJob.class);

    private final IngestMatchResultsUseCase ingestMatchResultsUseCase;
    private final int maxMatchesPerRun;

    public MatchResultIngestionJob(IngestMatchResultsUseCase ingestMatchResultsUseCase,
                                   @Value("${archivist.ingestion.max-matches-per-run:25}") int maxMatchesPerRun) {
        this.ingestMatchResultsUseCase = ingestMatchResultsUseCase;
        this.maxMatchesPerRun = maxMatchesPerRun;
    }

    // fixedDelay: la siguiente corrida empieza N ms después de que termina la anterior (nunca se solapan).
    @Scheduled(initialDelayString = "${archivist.ingestion.initial-delay-ms:60000}",
            fixedDelayString = "${archivist.ingestion.fixed-delay-ms:900000}")
    public void run() {
        try {
            IngestMatchResultsUseCase.Report report = ingestMatchResultsUseCase.ingestPending(maxMatchesPerRun);
            if (report.attempted() > 0) {
                log.info("Ingesta de Match Result: {} intentadas, {} guardadas, {} no encontradas, {} con error{}",
                        report.attempted(), report.saved(), report.notFound(), report.failed(),
                        report.stoppedByApi() ? " (detenida por la Halo API)" : "");
            }
        } catch (RuntimeException e) {
            log.error("La ingesta de Match Result falló: {}", e.getMessage(), e);
        }
    }
}
