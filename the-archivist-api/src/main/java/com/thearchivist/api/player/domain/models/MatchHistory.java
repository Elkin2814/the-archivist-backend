package com.thearchivist.api.player.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Modelo del caso de uso "historial de un jugador": una página de partidas
 * más los totales calculados sobre todo lo guardado.
 */
@AllArgsConstructor
@Getter
public class MatchHistory {
    private final Player player;
    private final List<PlayerMatch> matches;
    private final int page;
    private final int size;
    private final long totalMatches;
    private final long wins;
    private final long losses;

    public double getWinRate() {
        if (totalMatches == 0) return 0.0;
        return Math.round(wins * 1000.0 / totalMatches) / 10.0;
    }
}
