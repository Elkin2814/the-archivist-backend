package com.thearchivist.api.player.infrastructure.controllers.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resumen del jugador para la pantalla de perfil. Solo cuenta partidas de matchmaking.
 * Si el ID de un líder no está en el catálogo, key/faction/alias llegan nulos y name dice "Líder #N".
 */
public record PlayerSummaryResponse(
        String gamerTag,
        Instant lastSync,
        Totals overall,
        List<LeaderLine> byLeader,
        List<ModeLine> byGameMode,
        List<String> recentForm,
        Csr currentCsr
) {
    public record Totals(long matches, long wins, long losses, double winRate) {}

    public record LeaderLine(Integer leaderId, String key, String faction, String name, String alias,
                             long matches, long wins, long losses, double winRate) {}

    public record ModeLine(String gameMode, long matches, long wins, long losses, double winRate) {}

    public record Csr(Integer designation, Integer tier) {}
}
