package com.thearchivist.api.player.domain.models;

import com.thearchivist.api.stats.domain.models.WinRate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Dónde está un jugador en cada cola: su rango oficial actual (CSR/MMR exactos de su última partida), su
 * puesto en el leaderboard si lo tiene, su rendimiento con intervalo de confianza y cuánto confiar en ello.
 */
public record PlayerClassification(Player player, List<PlaylistClassification> playlists) {

    public enum Activity { ACTIVE, INACTIVE }

    /**
     * Rango del jugador tras su última partida de la cola. {@code mmrPercentile} (0 a 100) lo ubica entre los
     * participantes de nuestra base en esa cola; es relativo a la muestra rastreada, no a toda la población.
     */
    public record Standing(Integer designation, String designationName, Integer tier, Integer csr,
                           Integer percentToNextTier, Integer measurementMatchesRemaining,
                           Double mmr, Double mmrVariance, Double mmrPercentile, long percentileSample) {}

    /**
     * @param leaderboardRank puesto oficial (1 = primero) si figura en el top 250 de la cola
     * @param provisional     true si aún le faltan partidas de clasificación (el rango oficial no es firme)
     */
    public record PlaylistClassification(UUID playlistId, String playlistName, long matches, long wins, long losses,
                                         WinRate winRate, WinRate.Confidence confidence, Instant lastMatch,
                                         Activity activity, boolean provisional, Integer leaderboardRank,
                                         Standing standing) {}
}
