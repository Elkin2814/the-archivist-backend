package com.thearchivist.api.crawler.domain.models;

import java.time.Instant;

/**
 * Jugador rastreado en la clasificación propia: su rango tras la última partida de la cola (CSR y MMR exactos)
 * y su rendimiento en esa cola. {@code position} es el puesto entre los rastreados activos, no el oficial.
 */
public record TrackedPlayer(int position, String gamertag, Integer csrDesignation, String designationName,
                            Integer csrTier, Integer csr, Integer percentToNextTier, Double mmr,
                            long matches, long wins, Instant lastMatch) {}
