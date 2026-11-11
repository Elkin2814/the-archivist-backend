package com.thearchivist.api.player.domain.models;

import com.thearchivist.api.stats.domain.enums.StateMatch;
import com.thearchivist.api.stats.domain.models.CsrReading;
import com.thearchivist.api.stats.domain.models.MmrReading;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

/**
 * La participación de un jugador en una partida.
 * Los campos Integer y los bloques de CSR/MMR son nulos cuando la Halo API no los entrega
 * (por ejemplo CSR en partidas no clasificatorias).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlayerMatch {
    private Match match;
    private StateMatch stateMatch;
    private int leaderId;
    private boolean completedMatch;
    private Integer teamId;
    private Integer playerIndex;
    private Integer teamPlayerIndex;
    private Duration duration;
    private CsrReading previousCsr;
    private CsrReading updatedCsr;
    private MmrReading previousMmr;
    private MmrReading updatedMmr;
    private Integer previousXp;
    private Integer updatedXp;
}
