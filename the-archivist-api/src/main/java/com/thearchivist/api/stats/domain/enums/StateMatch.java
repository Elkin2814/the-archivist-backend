package com.thearchivist.api.stats.domain.enums;

/**
 * Resultado de la partida para el jugador (campo "PlayerMatchOutcome" de la Halo API).
 */
public enum StateMatch {
    UNKNOWN(0),
    VICTORY(1),
    DEFEAT(2),
    DRAW(3);

    private final int code;

    StateMatch(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static StateMatch fromOutcome(Integer outcome) {
        if (outcome == null) return UNKNOWN;
        for (StateMatch state : values()) {
            if (state.code == outcome) return state;
        }
        return UNKNOWN;
    }
}
