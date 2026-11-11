package com.thearchivist.api.crawler.domain.models;

/**
 * Fila de la clasificación de jugadores: puesto oficial y CSR, más cuántas partidas del jugador tenemos
 * guardadas ({@code matchesTracked}), que indica qué tan bien cubierto está en nuestra base.
 */
public record RankedPlayer(int rank, String gamertag, Integer csrDesignation, String designationName,
                           Integer csrTier, Integer csr, Integer percentToNextTier, long matchesTracked) {}
