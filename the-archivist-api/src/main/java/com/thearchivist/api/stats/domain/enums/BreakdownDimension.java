package com.thearchivist.api.stats.domain.enums;

/**
 * Ejes por los que se parte el rendimiento de un líder. La columna es de {@code v_valid_participants} y sale de
 * esta lista cerrada (nunca de un parámetro de la petición), así que se puede concatenar en el SQL sin riesgo.
 */
public enum BreakdownDimension {
    /** 1, 2 o 3 (1v1, 2v2, 3v3). */
    TEAM_SIZE("team_size", true),
    /** Código de {@link GameMode}: Deathmatch, Domination, Strongholds... */
    GAME_MODE("game_mode", false),
    /** Identificador del mapa (el mismo {@code id} de {@code /metadata/maps}). */
    MAP("map_id", false),
    /** Rango CSR al empezar la partida (0 Unranked ... 7 Champion). */
    RANK("csr_designation_before", true);

    private final String column;
    private final boolean naturalOrder;

    BreakdownDimension(String column, boolean naturalOrder) {
        this.column = column;
        this.naturalOrder = naturalOrder;
    }

    public String column() {
        return column;
    }

    /** True si el eje tiene un orden propio (tamaño de equipo, rango) que no debe reordenarse por win rate. */
    public boolean hasNaturalOrder() {
        return naturalOrder;
    }
}
