package com.thearchivist.api.player.infrastructure.controllers.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Una partida tal como la ve el frontend. No expone el JSON original de Halo.
 * Por ahora "map" y "leaderId" son los códigos crudos; se traducirán a nombres con Metadata.
 */
public record PlayerMatchDTO(
        UUID matchId,
        Instant date,
        String map,
        String gameMode,
        String matchType,
        int leaderId,
        String result,
        Long durationSeconds,
        Csr csrBefore,
        Csr csrAfter,
        Double mmrBefore,
        Double mmrAfter
) {
    public record Csr(Integer tier, Integer designation) {}
}
