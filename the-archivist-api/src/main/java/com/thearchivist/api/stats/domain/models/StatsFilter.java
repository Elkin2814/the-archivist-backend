package com.thearchivist.api.stats.domain.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Qué partidas entran en una estadística. Siempre se parte de las partidas válidas de matchmaking
 * ({@code v_valid_participants}); estos campos solo las acotan. Los nulos no filtran.
 *
 * @param minDesignation rango CSR mínimo al empezar la partida (0 Unranked ... 7 Champion), del jugador que se mide
 * @param maxDesignation rango CSR máximo, del jugador que se mide
 * @param teamSize       1, 2 o 3 (1v1, 2v2, 3v3)
 * @param gameMode       código de GameMode (3 Deathmatch, 4 Domination, 5 Strongholds...)
 * @param from           solo partidas empezadas desde esta fecha (por ejemplo, tras el último parche)
 * @param minSeconds     duración mínima de la partida; las muy cortas casi siempre son alguien que no carga y se rinde
 */
public record StatsFilter(UUID playlistId, Integer minDesignation, Integer maxDesignation, Integer teamSize,
                          Integer gameMode, Instant from, int minSeconds) {

    /** Duración mínima recomendada: en datos reales, el 7 % de las partidas normales dura menos de 2 minutos. */
    public static final int DEFAULT_MIN_SECONDS = 120;
}
