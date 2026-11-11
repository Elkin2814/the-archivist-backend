package com.thearchivist.api.crawler.domain.ports.out;

import com.thearchivist.api.crawler.domain.models.TrackedPlayer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Clasificación propia de los jugadores rastreados (capa 2), calculada con el CSR y el MMR exactos que traen sus
 * partidas. Cubre todos los rangos, no solo el top 250 del leaderboard oficial.
 */
public interface TrackedRankingPort {

    /**
     * Orden: designación, luego CSR (solo existe en Onyx y Champion), luego MMR; todos de mayor a menor. Solo
     * entran jugadores con al menos {@code minMatches} partidas decididas en la cola y alguna desde {@code since}
     * (a un jugador inactivo el rango guardado le puede haber quedado viejo).
     */
    List<TrackedPlayer> findTracked(UUID playlistId, int minMatches, Instant since, int limit, int offset);

    long countTracked(UUID playlistId, int minMatches, Instant since);
}
