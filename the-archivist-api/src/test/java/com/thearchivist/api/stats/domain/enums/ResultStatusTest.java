package com.thearchivist.api.stats.domain.enums;

import com.thearchivist.api.stats.domain.models.MatchParticipant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.thearchivist.api.stats.domain.enums.ResultStatus.ABNORMAL_END;
import static com.thearchivist.api.stats.domain.enums.ResultStatus.INCOMPLETE;
import static com.thearchivist.api.stats.domain.enums.ResultStatus.NO_WINNER;
import static com.thearchivist.api.stats.domain.enums.ResultStatus.VALID;
import static com.thearchivist.api.stats.domain.enums.ResultStatus.classify;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Casos reales observados en la Halo API (ver el comentario de {@link ResultStatus}). */
class ResultStatusTest {

    private static List<MatchParticipant> players(int... outcomes) {
        List<MatchParticipant> list = new ArrayList<>();
        for (int i = 0; i < outcomes.length; i++) {
            list.add(new MatchParticipant(i + 1, i < outcomes.length / 2 ? 1 : 2, i, 1, outcomes[i],
                    true, true, 10L, null, null, null, null));
        }
        return list;
    }

    @Test
    void normalMatchIsValid() {
        assertEquals(VALID, classify(true, 1, players(1, 1, 1, 2, 2, 2)));
        assertEquals(VALID, classify(true, 1, players(2, 2, 1, 1)));
    }

    @Test
    void serverCrashEveryoneDefeatedIsNotValid() {
        // MatchEndReason 3, VictoryCondition 0, equipos con MatchOutcome 0 y todos los jugadores en derrota.
        assertEquals(NO_WINNER, classify(true, 3, players(2, 2, 2, 2, 2, 2)));
        assertEquals(NO_WINNER, classify(true, 3, players(0, 0, 0, 0, 0, 0)));
    }

    @Test
    void bothTeamsDefeatedWithNormalEndIsNotValid() {
        assertEquals(NO_WINNER, classify(true, 1, players(2, 2, 2, 2, 2, 2)));
    }

    @Test
    void drawOrMissingLoserIsNotValid() {
        assertEquals(NO_WINNER, classify(true, 1, players(3, 3, 3, 3)));
        assertEquals(NO_WINNER, classify(true, 1, players(1, 1, 1)));
        assertEquals(NO_WINNER, classify(true, 1, players()));
    }

    @Test
    void winnerWithUnknownEndReasonIsExcludedUntilUnderstood() {
        assertEquals(ABNORMAL_END, classify(true, 2, players(1, 2)));
        assertEquals(ABNORMAL_END, classify(true, null, players(1, 2)));
    }

    @Test
    void incompleteMatchIsNotValid() {
        assertEquals(INCOMPLETE, classify(false, 1, players(1, 2)));
        assertEquals(INCOMPLETE, classify(null, 1, players(1, 2)));
    }

    @Test
    void teamOutcomeDecidesWhenAPlayerLeftTheWinningTeam() {
        // La documentación de la API: quien abandona figura en derrota aunque su equipo gane. Si el único
        // jugador del equipo ganador (1v1) se fue, por jugadores parecería doble derrota.
        List<MatchParticipant> onlyLosses = players(2, 2);
        assertEquals(NO_WINNER, classify(true, 1, onlyLosses));
        assertEquals(VALID, classify(true, 1, onlyLosses, List.of(1, 2)));
    }

    @Test
    void teamOutcomesWithoutWinnerAreStillACrash() {
        // Caída de servidor: ambos equipos con MatchOutcome 0.
        assertEquals(NO_WINNER, classify(true, 3, players(2, 2, 2, 2), List.of(0, 0)));
        assertEquals(NO_WINNER, classify(true, 1, players(1, 2), List.of(0, 0)));
    }

    @Test
    void emptyTeamOutcomesFallBackToPlayers() {
        assertEquals(VALID, classify(true, 1, players(1, 2), List.of()));
        assertEquals(VALID, classify(true, 1, players(1, 2), null));
    }
}
