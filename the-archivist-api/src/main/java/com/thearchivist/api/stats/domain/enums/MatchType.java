package com.thearchivist.api.stats.domain.enums;

/**
 * Tipos de partida de Halo Wars 2 (campo "MatchType" de la Halo API).
 */
public enum MatchType {
    UNKNOWN(0),
    CAMPAIGN(1),
    CUSTOM(2),
    MATCHMAKING(3);

    private final int code;

    MatchType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MatchType fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        for (MatchType type : values()) {
            if (type.code == code) return type;
        }
        return UNKNOWN;
    }
}
