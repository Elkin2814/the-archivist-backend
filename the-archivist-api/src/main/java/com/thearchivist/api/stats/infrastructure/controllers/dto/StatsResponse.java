package com.thearchivist.api.stats.infrastructure.controllers.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Respuestas de {@code /stats}: porcentajes con un decimal, siempre junto al tamaño de la muestra. */
public final class StatsResponse {

    private StatsResponse() {}

    public record Filters(UUID playlistId, Integer minDesignation, Integer maxDesignation, Integer teamSize,
                          Integer gameMode, LocalDate from, int minSeconds) {}

    public record Sample(long matches, long participants, Instant oldest, Instant newest) {}

    public record LeaderRef(int leaderId, String name, String faction, String displayName, String alias) {}

    /** Un líder: partidas, victorias, win rate con intervalo de Wilson 95 %, popularidad y veredicto frente al 50 %. */
    public record LeaderLine(LeaderRef leader, long matches, long wins, double winRate, double winRateLow,
                             double winRateHigh, double pickRate, String confidence, String verdict) {}

    public record LeadersReport(Filters filters, Sample sample, long minMatches, List<LeaderLine> leaders,
                                List<String> notes) {}

    /** Un líder frente a (o junto a) otro. */
    public record PairLine(LeaderRef other, long matches, long wins, double winRate, double winRateLow,
                           double winRateHigh, String confidence, String verdict) {}

    /** Un valor de un eje (clave en bruto: tamaño de equipo, código de modo, id de mapa o rango CSR). */
    public record BreakdownLine(String key, long matches, long wins, double winRate, double winRateLow,
                                double winRateHigh, String confidence, String verdict) {}

    /** Rendimiento de un líder: total y por tamaño de equipo, modo de juego, mapa y rango. */
    public record BreakdownReport(Filters filters, Sample sample, LeaderRef leader, long minMatches,
                                  LeaderLine overall, List<BreakdownLine> byTeamSize, List<BreakdownLine> byGameMode,
                                  List<BreakdownLine> byMap, List<BreakdownLine> byRank, List<String> notes) {}

    public record PairsReport(Filters filters, Sample sample, LeaderRef leader, long minMatches,
                              List<PairLine> pairs, List<String> notes) {}
}
