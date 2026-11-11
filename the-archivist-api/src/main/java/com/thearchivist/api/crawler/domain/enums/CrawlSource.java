package com.thearchivist.api.crawler.domain.enums;

/** De dónde salió el jugador de la cola. */
public enum CrawlSource {
    /** Del leaderboard oficial (top 250 por cola). */
    LEADERBOARD,
    /** De la configuración (archivist.crawl.seed-gamertags). */
    MANUAL
}
