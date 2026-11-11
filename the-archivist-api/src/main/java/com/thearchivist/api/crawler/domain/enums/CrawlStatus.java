package com.thearchivist.api.crawler.domain.enums;

/** Estado de un jugador en la cola de rastreo (tabla crawl_players). */
public enum CrawlStatus {
    /** Falta recorrer su historial (o seguir donde se quedó). */
    PENDING,
    /** Al día; se vuelve a revisar cuando llegue next_crawl_at. */
    DONE,
    /** La Halo API no conoce ese gamertag. */
    NOT_FOUND,
    /** Agotó los intentos por errores repetidos. */
    FAILED
}
