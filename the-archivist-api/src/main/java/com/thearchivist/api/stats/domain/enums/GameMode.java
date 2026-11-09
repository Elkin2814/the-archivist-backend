package com.thearchivist.api.stats.domain.enums;

/**
 * Modos de juego de Halo Wars 2 (campo "GameMode" de la Halo API).
 */
public enum GameMode {
    UNKNOWN(0),
    CAMPAIGN_SOLO(1),
    CAMPAIGN_COOPERATIVE(2),
    DEATHMATCH(3),
    DOMINATION(4),
    STRONGHOLDS(5),
    BLITZ(6),
    FIREFIGHT(7),
    NORMAL_TUTORIAL(8),
    BLITZ_TUTORIAL(9),
    TERMINUS_FIREFIGHT(10);

    private final int code;

    GameMode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static GameMode fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        for (GameMode mode : values()) {
            if (mode.code == code) return mode;
        }
        return UNKNOWN;
    }
}
