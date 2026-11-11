package com.thearchivist.api.player.infrastructure.controllers.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Clasificación de un jugador por cola. Porcentajes con un decimal. */
public record PlayerClassificationResponse(String gamertag, Instant lastSync, List<PlaylistLine> playlists,
                                           List<String> notes) {

    /**
     * @param confidence  qué tan firme es el win rate (HIGH/MEDIUM/LOW según el ancho del intervalo)
     * @param provisional aún le faltan partidas de clasificación: su rango oficial no es definitivo
     * @param activity    ACTIVE si jugó en los últimos 30 días; si no, el rango guardado puede estar desactualizado
     */
    public record PlaylistLine(UUID playlistId, String playlistName, long matches, long wins, long losses,
                               double winRate, double winRateLow, double winRateHigh, String confidence,
                               Instant lastMatch, String activity, boolean provisional,
                               Integer leaderboardRank, Standing standing) {}

    public record Standing(Integer designation, String designationName, Integer tier, Integer csr,
                           Integer percentToNextTier, Integer measurementMatchesRemaining,
                           Double mmr, Double mmrVariance, Double mmrPercentile, long percentileSample) {}
}
