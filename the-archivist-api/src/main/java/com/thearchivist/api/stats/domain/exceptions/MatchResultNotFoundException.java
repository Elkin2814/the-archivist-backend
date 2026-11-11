package com.thearchivist.api.stats.domain.exceptions;

import java.util.UUID;

/**
 * La Halo API no tiene (o ya no tiene) el resultado de esta partida.
 */
public class MatchResultNotFoundException extends RuntimeException {

    public MatchResultNotFoundException(UUID matchId) {
        super("La Halo API no tiene el resultado de la partida " + matchId);
    }
}
