package com.thearchivist.api.stats.application.services;

import com.thearchivist.api.stats.domain.enums.BreakdownDimension;
import com.thearchivist.api.stats.domain.models.BreakdownRow;
import com.thearchivist.api.stats.domain.models.LeaderRow;
import com.thearchivist.api.stats.domain.models.PairRow;
import com.thearchivist.api.stats.domain.models.StatsFilter;
import com.thearchivist.api.stats.domain.models.StatsSample;
import com.thearchivist.api.stats.domain.models.WinRate.Verdict;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase.BreakdownStat;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase.LeaderBreakdown;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase.LeaderStat;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase.LeadersReport;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase.PairReport;
import com.thearchivist.api.stats.domain.ports.out.StatsRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsServiceTest {

    private static final StatsFilter FILTER = new StatsFilter(null, null, null, null, null, null, 120);

    /** Repositorio en memoria con datos fijos. */
    private static StatsRepositoryPort repo(long participants, List<LeaderRow> leaders, List<PairRow> pairs) {
        return repo(participants, leaders, pairs, Map.of());
    }

    private static StatsRepositoryPort repo(long participants, List<LeaderRow> leaders, List<PairRow> pairs,
                                            Map<BreakdownDimension, List<BreakdownRow>> axes) {
        return new StatsRepositoryPort() {
            public StatsSample sample(StatsFilter f) {
                return new StatsSample(participants / 6, participants, Instant.EPOCH, Instant.now());
            }
            public List<LeaderRow> leaderRows(StatsFilter f) { return leaders; }
            public List<PairRow> matchups(StatsFilter f, int leaderId) { return pairs; }
            public List<PairRow> synergies(StatsFilter f, int leaderId) { return pairs; }
            public List<BreakdownRow> breakdown(StatsFilter f, int leaderId, BreakdownDimension d) {
                return axes.getOrDefault(d, List.of());
            }
        };
    }

    @Test
    void breakdownKeepsNaturalOrderForTeamSizeAndRankAndRanksMapsByLowerBound() {
        Map<BreakdownDimension, List<BreakdownRow>> axes = Map.of(
                BreakdownDimension.TEAM_SIZE, List.of(new BreakdownRow("3", 300, 150), new BreakdownRow("1", 100, 70),
                        new BreakdownRow("2", 200, 100)),
                BreakdownDimension.RANK, List.of(new BreakdownRow("7", 50, 30), new BreakdownRow("2", 60, 30)),
                BreakdownDimension.MAP, List.of(new BreakdownRow("weak", 400, 160), new BreakdownRow("tiny", 2, 2),
                        new BreakdownRow("strong", 400, 240)));
        LeaderBreakdown report = new StatsService(repo(1200, List.of(new LeaderRow(1, 600, 330)), List.of(), axes))
                .breakdown(FILTER, 1, 30);

        assertEquals(1, report.leaderId());
        assertEquals(600, report.overall().matches());
        assertEquals(0.5, report.overall().pickRate(), 0.0001);
        // Tamaño de equipo y rango: orden natural (1v1, 2v2, 3v3 / rango 2, rango 7), no por win rate.
        assertEquals(List.of("1", "2", "3"), report.byTeamSize().stream().map(BreakdownStat::key).toList());
        assertEquals(List.of("2", "7"), report.byRank().stream().map(BreakdownStat::key).toList());
        // Mapas: de mejor a peor, y la muestra diminuta al final aunque tenga 100 % crudo.
        assertEquals(List.of("strong", "weak", "tiny"), report.byMap().stream().map(BreakdownStat::key).toList());
        assertEquals(Verdict.ABOVE_AVERAGE, report.byMap().get(0).verdict());
        assertEquals(Verdict.BELOW_AVERAGE, report.byMap().get(1).verdict());
        assertEquals(Verdict.INSUFFICIENT_DATA, report.byMap().get(2).verdict());
        assertTrue(report.byGameMode().isEmpty());
    }

    @Test
    void breakdownOfALeaderWithoutMatchesIsEmptyButWellFormed() {
        LeaderBreakdown report = new StatsService(repo(0, List.of(), List.of())).breakdown(FILTER, 5, 30);
        assertEquals(0, report.overall().matches());
        assertEquals(Verdict.INSUFFICIENT_DATA, report.overall().verdict());
        assertTrue(report.byTeamSize().isEmpty() && report.byMap().isEmpty() && report.byRank().isEmpty());
    }

    @Test
    void alwaysReturnsAllSixteenLeadersEvenWithoutMatches() {
        LeadersReport report = new StatsService(repo(0, List.of(), List.of())).leaders(FILTER, 30);
        assertEquals(16, report.leaders().size());
        assertTrue(report.leaders().stream().allMatch(l -> l.verdict() == Verdict.INSUFFICIENT_DATA));
        assertTrue(report.leaders().stream().allMatch(l -> l.pickRate() == 0.0));
    }

    @Test
    void rankedByLowerBoundAndSmallSamplesGoLast() {
        // Cutter (1): 60 % en 1000 -> ventaja clara. Isabel (2): 100 % en 3 -> muestra insuficiente.
        // Anders (3): 50 % en 200 -> promedio.
        List<LeaderRow> rows = List.of(new LeaderRow(1, 1000, 600), new LeaderRow(2, 3, 3), new LeaderRow(3, 200, 100));
        LeadersReport report = new StatsService(repo(1203, rows, List.of())).leaders(FILTER, 30);

        List<LeaderStat> top = report.leaders();
        assertEquals(1, top.get(0).leaderId());
        assertEquals(Verdict.ABOVE_AVERAGE, top.get(0).verdict());
        assertEquals(3, top.get(1).leaderId());
        assertEquals(Verdict.AVERAGE, top.get(1).verdict());
        // Isabel, con 100 % "crudo", no puede quedar por encima de quien tiene muestra de verdad.
        int isabel = indexOf(top, 2);
        assertTrue(isabel >= 2, "Isabel con 3 partidas no debe liderar la tabla");
        assertEquals(Verdict.INSUFFICIENT_DATA, top.get(isabel).verdict());
    }

    @Test
    void pickRateIsShareOfPlayerSlots() {
        LeadersReport report = new StatsService(repo(2000, List.of(new LeaderRow(1, 500, 250)), List.of()))
                .leaders(FILTER, 30);
        LeaderStat cutter = report.leaders().get(indexOf(report.leaders(), 1));
        assertEquals(0.25, cutter.pickRate(), 0.0001);
    }

    @Test
    void leaderUnknownToCatalogIsKept() {
        LeadersReport report = new StatsService(repo(100, List.of(new LeaderRow(99, 50, 25)), List.of()))
                .leaders(FILTER, 30);
        assertEquals(17, report.leaders().size());
        assertTrue(indexOf(report.leaders(), 99) >= 0);
    }

    @Test
    void matchupsKeepOrderingByLowerBound() {
        List<PairRow> pairs = List.of(new PairRow(5, 400, 240), new PairRow(6, 400, 160), new PairRow(7, 2, 2));
        PairReport report = new StatsService(repo(0, List.of(), pairs)).matchups(FILTER, 1, 30);
        assertEquals(1, report.leaderId());
        assertEquals(5, report.pairs().get(0).otherLeaderId());
        assertEquals(Verdict.ABOVE_AVERAGE, report.pairs().get(0).verdict());
        assertEquals(6, report.pairs().get(1).otherLeaderId());
        assertEquals(Verdict.BELOW_AVERAGE, report.pairs().get(1).verdict());
        assertEquals(7, report.pairs().get(2).otherLeaderId());
        assertEquals(Verdict.INSUFFICIENT_DATA, report.pairs().get(2).verdict());
    }

    private static int indexOf(List<LeaderStat> list, int leaderId) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).leaderId() == leaderId) return i;
        return -1;
    }
}
