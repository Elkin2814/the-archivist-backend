package com.thearchivist.api.stats.domain.ports.in;

import com.thearchivist.api.stats.domain.models.StatsFilter;
import com.thearchivist.api.stats.domain.models.StatsSample;
import com.thearchivist.api.stats.domain.models.WinRate;

import java.util.List;

/** Win rate y popularidad por líder, matchups y sinergias, con intervalos de confianza. */
public interface GetStatsUseCase {

    /** @param pickRate parte de los puestos de jugador de la muestra que usó este líder (0 a 1) */
    record LeaderStat(int leaderId, long matches, long wins, double winRate, double winRateLow, double winRateHigh,
                      double pickRate, WinRate.Verdict verdict) {}

    /** Un líder frente a (o junto a) otro. */
    record PairStat(int otherLeaderId, long matches, long wins, double winRate, double winRateLow,
                    double winRateHigh, WinRate.Verdict verdict) {}

    /** Un valor de un eje (un mapa, un tamaño de equipo...) con win rate e intervalo de Wilson. */
    record BreakdownStat(String key, long matches, long wins, double winRate, double winRateLow,
                         double winRateHigh, WinRate.Verdict verdict) {}

    /** Rendimiento de un líder: total y partido por tamaño de equipo, modo de juego, mapa y rango. */
    record LeaderBreakdown(StatsSample sample, int leaderId, long minMatches, LeaderStat overall,
                           List<BreakdownStat> byTeamSize, List<BreakdownStat> byGameMode,
                           List<BreakdownStat> byMap, List<BreakdownStat> byRank) {}

    record LeadersReport(StatsSample sample, long minMatches, List<LeaderStat> leaders) {}

    record PairReport(StatsSample sample, int leaderId, long minMatches, List<PairStat> pairs) {}

    LeadersReport leaders(StatsFilter filter, long minMatches);

    PairReport matchups(StatsFilter filter, int leaderId, long minMatches);

    PairReport synergies(StatsFilter filter, int leaderId, long minMatches);

    LeaderBreakdown breakdown(StatsFilter filter, int leaderId, long minMatches);
}
