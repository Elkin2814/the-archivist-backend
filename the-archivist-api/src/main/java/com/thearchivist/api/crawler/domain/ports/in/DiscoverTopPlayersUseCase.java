package com.thearchivist.api.crawler.domain.ports.in;

import com.thearchivist.api.crawler.domain.models.Reports;

import java.util.List;

/** Descarga el leaderboard oficial de las colas clasificatorias y mete a los mejores en la cola de rastreo. */
public interface DiscoverTopPlayersUseCase {

    /**
     * @param perPlaylist   cuántos puestos pedir por cola (la API entrega como máximo 250)
     * @param extraGamertags jugadores que se añaden a mano a la cola, además del leaderboard
     */
    Reports.Discovery discover(int perPlaylist, List<String> extraGamertags);
}
