package com.thearchivist.api.stats.domain.models;

import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.MatchType;
import com.thearchivist.api.stats.domain.enums.ResultStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resultado completo de una partida ("Match Result"): datos de la partida y TODOS sus jugadores.
 * No contiene gamertags a propósito: es material de la capa permanente de análisis.
 *
 * @param complete        IsMatchComplete de la Halo API
 * @param endReason       MatchEndReason (código crudo)
 * @param victoryCondition VictoryCondition (código crudo)
 * @param teamOutcomes  MatchOutcome de cada equipo (clave: id del equipo); vacío si la API no lo trajo
 */
public record MatchDetail(
        UUID matchId,
        MatchType matchType,
        GameMode gameMode,
        UUID seasonId,
        UUID playlistId,
        String mapId,
        Instant startDate,
        Long durationSeconds,
        Boolean complete,
        Integer endReason,
        Integer victoryCondition,
        List<MatchParticipant> participants,
        Map<Integer, Integer> teamOutcomes
) {

    public MatchDetail {
        teamOutcomes = teamOutcomes == null ? Map.of() : Map.copyOf(teamOutcomes);
    }

    /** Si la partida cuenta para las estadísticas (ver {@link ResultStatus}). */
    public ResultStatus resultStatus() {
        return ResultStatus.classify(complete, endReason, participants, teamOutcomes.values());
    }
}
