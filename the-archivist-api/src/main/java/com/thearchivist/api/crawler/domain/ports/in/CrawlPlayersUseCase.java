package com.thearchivist.api.crawler.domain.ports.in;

import com.thearchivist.api.crawler.domain.models.Reports;

/** Recorre el historial de los jugadores de la cola y guarda sus partidas (las completa luego la ingesta). */
public interface CrawlPlayersUseCase {

    /**
     * @param maxPlayers       cuántos jugadores trabajar en esta corrida
     * @param maxPagesPerPlayer páginas de 25 partidas por jugador y corrida (un historial largo se recorre en varias)
     */
    Reports.Crawl crawl(int maxPlayers, int maxPagesPerPlayer);
}
