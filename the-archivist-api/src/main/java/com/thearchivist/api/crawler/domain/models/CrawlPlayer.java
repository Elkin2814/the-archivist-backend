package com.thearchivist.api.crawler.domain.models;

import com.thearchivist.api.crawler.domain.enums.CrawlStatus;

/**
 * Jugador de la cola de rastreo. Lleva gamertag, así que vive en la capa 2 (temporal), nunca en la de análisis.
 *
 * @param historyCursor   posición del historial (start) desde la que continuar; solo vale si no está completo
 * @param historyComplete true si ya se recorrió todo y solo hace falta traer lo nuevo
 */
public record CrawlPlayer(long id, String gamertag, CrawlStatus status, int historyCursor, boolean historyComplete,
                          int attempts) {}
