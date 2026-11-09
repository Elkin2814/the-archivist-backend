package com.thearchivist.api.player.domain.models;

import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.StateMatch;

import java.util.List;

/**
 * Resumen de un jugador calculado sobre sus partidas de matchmaking guardadas:
 * totales, rendimiento por líder y por modo, forma reciente y último rango conocido.
 */
public record PlayerSummary(
        Player player,
        Totals overall,
        List<LeaderStat> byLeader,
        List<ModeStat> byGameMode,
        List<StateMatch> recentForm,
        Csr currentCsr
) {
    public record Totals(long matches, long wins, long losses) {
        /** Porcentaje de victorias con un decimal (0 si no hay partidas). */
        public double winRate() {
            if (matches == 0) return 0.0;
            return Math.round(wins * 1000.0 / matches) / 10.0;
        }
    }

    public record LeaderStat(int leaderId, Totals totals) {}

    public record ModeStat(GameMode gameMode, Totals totals) {}

    /** Rango CSR (designación y tier) tras la última partida clasificatoria guardada. */
    public record Csr(Integer designation, Integer tier) {}
}
