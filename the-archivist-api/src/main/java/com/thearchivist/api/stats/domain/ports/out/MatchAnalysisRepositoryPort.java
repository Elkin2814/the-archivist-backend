package com.thearchivist.api.stats.domain.ports.out;

import com.thearchivist.api.stats.domain.models.MatchDetail;
import com.thearchivist.api.stats.domain.models.MatchParticipant;

import java.util.List;
import java.util.UUID;

/**
 * Persistencia de la capa permanente de análisis (matches y match_participants), sin gamertags.
 */
public interface MatchAnalysisRepositoryPort {

    /**
     * Guarda la fila de un jugador en una partida. Idempotente: si ya existía, solo completa los
     * campos que estaban vacíos (los hechos de una partida terminada no cambian).
     * La fila de {@code matches} debe existir.
     */
    void saveParticipant(UUID matchId, MatchParticipant participant);

    /** Partidas de matchmaking a las que aún no se les pidió Match Result, las más recientes primero. */
    List<UUID> findMatchesPendingResult(int limit, int maxAttempts);

    /** Completa la partida con los datos de Match Result, guarda a todos sus jugadores y la saca de la cola. */
    void saveMatchDetail(MatchDetail detail);

    /** Anota un intento fallido definitivo (por ejemplo 404) para no reintentar eternamente. */
    void registerFailedAttempt(UUID matchId);
}
