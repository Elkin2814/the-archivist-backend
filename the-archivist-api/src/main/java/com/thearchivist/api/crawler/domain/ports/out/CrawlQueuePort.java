package com.thearchivist.api.crawler.domain.ports.out;

import com.thearchivist.api.crawler.domain.enums.CrawlSource;
import com.thearchivist.api.crawler.domain.enums.CrawlStatus;
import com.thearchivist.api.crawler.domain.models.CrawlPlayer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Cola temporal de jugadores a rastrear (capa 2: lleva gamertag). */
public interface CrawlQueuePort {

    record Seed(String gamertag, CrawlSource source, int priority) {}

    /** Añade jugadores nuevos; si ya estaban, solo sube la prioridad. Devuelve cuántos eran nuevos. */
    int enqueue(List<Seed> seeds);

    /** Siguiente jugador por recorrer (pendiente, o al día pero ya vencido), o vacío si no hay trabajo. */
    Optional<CrawlPlayer> claimNext(Instant now);

    /** Guarda el avance de un recorrido que quedó a medias; sigue PENDING. */
    void saveProgress(long id, int cursor, int newMatches);

    /** Historial al día: pasa a DONE y se vuelve a revisar en {@code nextCrawlAt}. */
    void markCompleted(long id, int newMatches, Instant nextCrawlAt);

    /** La Halo API no conoce al jugador. */
    void markNotFound(long id, String message);

    /** Error con ese jugador: suma un intento y, al llegar a {@code maxAttempts}, pasa a FAILED. */
    void markFailed(long id, String message, int maxAttempts);

    /**
     * Error pasajero del servidor con ese jugador (5xx): conserva su estado y sus intentos, pero lo manda al final de la
     * cola para que no bloquee a los demás. Se vuelve a intentar cuando le toque de nuevo; nunca queda excluido.
     */
    void postpone(long id, String message);

    Map<CrawlStatus, Long> countByStatus();

    /** Borra a quien no se ha tocado desde {@code cutoff}. Devuelve cuántos. */
    int purgeStale(Instant cutoff);
}
