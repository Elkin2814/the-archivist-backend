package com.thearchivist.api.crawler.domain.models;

/** Resúmenes de una corrida, para el log y las pruebas. */
public final class Reports {

    private Reports() {
    }

    /**
     * @param playlists     colas consultadas
     * @param entries       puestos guardados
     * @param newPlayers    jugadores nuevos en la cola de rastreo
     * @param stoppedByApi  true si se cortó porque la Halo API no estaba disponible
     */
    public record Discovery(int playlists, int entries, int newPlayers, boolean stoppedByApi) {}

    /**
     * @param players       jugadores trabajados (una pasada por jugador)
     * @param pages         páginas de historial pedidas
     * @param newMatches    partidas nuevas guardadas
     * @param completed     jugadores cuyo historial quedó al día
     * @param stoppedByApi  true si se cortó porque la Halo API no estaba disponible
     */
    public record Crawl(int players, int pages, int newMatches, int completed, boolean stoppedByApi) {}
}
