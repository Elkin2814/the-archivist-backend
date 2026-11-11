package com.thearchivist.api.stats.domain.models;

import java.util.Map;

/**
 * Un jugador dentro de una partida, SIN identidad: solo el índice que ocupó, su líder, el resultado y su rango.
 * Es la fila de la capa permanente de análisis (match_participants).
 *
 * @param playerIndex índice del jugador en la partida (clave junto con el MatchId)
 * @param outcome     código de PlayerMatchOutcome de la Halo API (1 victoria, 2 derrota, 3 empate, 0 desconocido)
 * @param human       false si el jugador era una IA
 * @param powersCast  poderes de líder que lanzó (clave del poder, veces lanzado); vacío si no se conoce
 * @param teamOutcome MatchOutcome de su EQUIPO (solo lo trae Match Result; nulo si no se conoce). Puede diferir de
 *                    {@code outcome}: el historial marca derrota a quien abandona aunque su equipo gane
 * @param teamSize    TeamSize de su equipo (1, 2 o 3 según el modo; solo lo trae Match Result)
 */
public record MatchParticipant(
        int playerIndex,
        Integer teamId,
        Integer teamPlayerIndex,
        int leaderId,
        int outcome,
        boolean completedMatch,
        boolean human,
        Long durationSeconds,
        CsrReading csrBefore,
        CsrReading csrAfter,
        MmrReading mmrBefore,
        MmrReading mmrAfter,
        Map<String, Integer> powersCast,
        Integer teamOutcome,
        Integer teamSize
) {

    public MatchParticipant {
        powersCast = powersCast == null ? Map.of() : Map.copyOf(powersCast);
    }

    /** Fila que viene del historial: no trae los poderes lanzados. */
    public MatchParticipant(int playerIndex, Integer teamId, Integer teamPlayerIndex, int leaderId, int outcome,
                            boolean completedMatch, boolean human, Long durationSeconds,
                            CsrReading csrBefore, CsrReading csrAfter, MmrReading mmrBefore, MmrReading mmrAfter) {
        this(playerIndex, teamId, teamPlayerIndex, leaderId, outcome, completedMatch, human, durationSeconds,
                csrBefore, csrAfter, mmrBefore, mmrAfter, Map.of(), null, null);
    }
}
