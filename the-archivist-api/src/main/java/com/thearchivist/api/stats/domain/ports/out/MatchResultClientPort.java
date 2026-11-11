package com.thearchivist.api.stats.domain.ports.out;

import com.thearchivist.api.stats.domain.models.MatchDetail;

import java.util.UUID;

/**
 * Salida hacia "Halo Wars 2 - Match Result".
 */
public interface MatchResultClientPort {

    /**
     * @throws com.thearchivist.api.stats.domain.exceptions.MatchResultNotFoundException si la Halo API no conoce la partida
     * @throws com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException si la API no se puede usar ahora
     */
    MatchDetail fetchMatchResult(UUID matchId);
}
