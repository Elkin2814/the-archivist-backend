package com.thearchivist.api.stats.domain.enums;

import com.thearchivist.api.stats.domain.models.MatchParticipant;

import java.util.Collection;

/**
 * Si el resultado de una partida sirve para estadísticas. Se calcula una vez, al ingerir "Match Result",
 * y se guarda en {@code matches.result_status}. Las partidas NO válidas se conservan (sirven, por ejemplo,
 * para medir con qué líderes o poderes se cae el servidor), pero nunca entran en winrates ni popularidad.
 *
 * <p>Lo que se observó en datos reales de la Halo API (1.090 partidas de matchmaking de una cuenta):
 * <ul>
 *   <li>Partida normal: {@code MatchEndReason = 1}, un equipo con {@code MatchOutcome = 1} y el otro con 2.</li>
 *   <li>Caída de servidor / conexión: {@code MatchEndReason = 3}, {@code VictoryCondition = 0}, los dos equipos
 *       con {@code MatchOutcome = 0}, y TODOS los jugadores con {@code PlayerMatchOutcome = 2} (derrota para ambos).
 *       Dura lo normal (mediana ~16 min), así que la duración no la detecta.</li>
 *   <li>También hay partidas con fin normal pero donde ambos equipos figuran en derrota.</li>
 *   <li>{@code PlayerCompletedMatch = false} NO indica error: lo tiene quien se rinde o se va (3 de cada 4
 *       jugadores con ese valor estaban en el equipo perdedor de una partida normal).</li>
 *   <li>{@code IsMatchComplete} vale true incluso en las caídas, por eso no basta.</li>
 * </ul>
 */
public enum ResultStatus {

    /** Hay un ganador y un perdedor, el fin fue normal y la partida está completa. Es la única que cuenta. */
    VALID,

    /** Nadie ganó (caída de servidor, todos desconectados, doble derrota) o no hay ningún perdedor. */
    NO_WINNER,

    /** Hay ganador, pero {@code MatchEndReason} no es el fin normal. Se excluye hasta entender ese código. */
    ABNORMAL_END,

    /** La Halo API marca la partida como no completa. */
    INCOMPLETE;

    /** {@code MatchEndReason} de una partida que terminó con normalidad. */
    public static final int NORMAL_END_REASON = 1;

    /**
     * Clasifica una partida con los datos de Match Result. Las IA cuentan igual que los humanos: el ganador
     * se decide por equipo. El orden importa: primero la falta de ganador, que es lo más informativo.
     *
     * <p>Se usa el resultado de los EQUIPOS cuando la API lo trae: el de un jugador puede ser derrota aunque su
     * equipo gane (quien abandona). Sin resultado de equipos se recurre al de los jugadores.
     */
    public static ResultStatus classify(Boolean complete, Integer endReason, Collection<MatchParticipant> participants,
                                        Collection<Integer> teamOutcomes) {
        boolean hasWinner = false;
        boolean hasLoser = false;
        if (teamOutcomes != null && !teamOutcomes.isEmpty()) {
            for (Integer outcome : teamOutcomes) {
                if (outcome == null) continue;
                if (outcome == StateMatch.VICTORY.getCode()) hasWinner = true;
                if (outcome == StateMatch.DEFEAT.getCode()) hasLoser = true;
            }
        } else {
            for (MatchParticipant p : participants) {
                if (p.outcome() == StateMatch.VICTORY.getCode()) hasWinner = true;
                if (p.outcome() == StateMatch.DEFEAT.getCode()) hasLoser = true;
            }
        }
        if (!hasWinner || !hasLoser) return NO_WINNER;
        if (!Boolean.TRUE.equals(complete)) return INCOMPLETE;
        if (endReason == null || endReason != NORMAL_END_REASON) return ABNORMAL_END;
        return VALID;
    }

    /** Igual que {@link #classify(Boolean, Integer, Collection, Collection)} usando solo el resultado de los jugadores. */
    public static ResultStatus classify(Boolean complete, Integer endReason, Collection<MatchParticipant> participants) {
        return classify(complete, endReason, participants, null);
    }
}
