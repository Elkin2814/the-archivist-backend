package com.thearchivist.api.stats.domain.ports.in;

/**
 * Llena la capa de análisis llamando a Match Result una sola vez por partida pendiente.
 */
public interface IngestMatchResultsUseCase {

    /**
     * Procesa hasta {@code maxMatches} partidas pendientes, las más recientes primero.
     * Se detiene sola si la Halo API deja de responder (límite de llamadas, llave inválida, caída).
     */
    Report ingestPending(int maxMatches);

    /**
     * @param attempted     partidas que se intentaron
     * @param saved         partidas guardadas con todos sus jugadores
     * @param notFound      partidas que la Halo API no conoce (se anota el intento)
     * @param failed        partidas que llegaron pero no se pudieron guardar (se anota el intento)
     * @param stoppedByApi  true si se cortó la corrida porque la Halo API no estaba disponible
     */
    record Report(int attempted, int saved, int notFound, int failed, boolean stoppedByApi) {}
}
