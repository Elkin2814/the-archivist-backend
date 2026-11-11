package com.thearchivist.api.stats.application.services;

import com.thearchivist.api.leader.domain.models.LeaderCatalog;
import com.thearchivist.api.stats.domain.enums.BreakdownDimension;
import com.thearchivist.api.stats.domain.models.BreakdownRow;
import com.thearchivist.api.stats.domain.models.LeaderRow;
import com.thearchivist.api.stats.domain.models.PairRow;
import com.thearchivist.api.stats.domain.models.StatsFilter;
import com.thearchivist.api.stats.domain.models.StatsSample;
import com.thearchivist.api.stats.domain.models.WinRate;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase;
import com.thearchivist.api.stats.domain.ports.out.StatsRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService implements GetStatsUseCase {

    private final StatsRepositoryPort repository;

    public StatsService(StatsRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public LeadersReport leaders(StatsFilter filter, long minMatches) {
        StatsSample sample = repository.sample(filter);
        Map<Integer, LeaderRow> rows = new HashMap<>();
        for (LeaderRow row : repository.leaderRows(filter)) rows.put(row.leaderId(), row);

        // Siempre salen los 16 líderes (aunque no tengan partidas) para que la tabla sea estable.
        List<LeaderStat> stats = new ArrayList<>();
        for (LeaderCatalog leader : LeaderCatalog.values()) {
            Integer id = leader.getApiId();
            if (id == null) continue;
            LeaderRow row = rows.remove(id);
            stats.add(toLeaderStat(id, row == null ? 0 : row.matches(), row == null ? 0 : row.wins(),
                    sample.participants(), minMatches));
        }
        // Líderes que la API devolvió y no están en el catálogo (por si Halo añade uno): no se descartan.
        for (LeaderRow row : rows.values()) {
            stats.add(toLeaderStat(row.leaderId(), row.matches(), row.wins(), sample.participants(), minMatches));
        }
        stats.sort(Comparator
                .comparing((LeaderStat s) -> s.verdict() == WinRate.Verdict.INSUFFICIENT_DATA)
                .thenComparing(Comparator.comparingDouble(LeaderStat::winRateLow).reversed())
                .thenComparing(Comparator.comparingLong(LeaderStat::matches).reversed()));
        return new LeadersReport(sample, minMatches, stats);
    }

    @Override
    public PairReport matchups(StatsFilter filter, int leaderId, long minMatches) {
        return pairReport(filter, leaderId, minMatches, repository.matchups(filter, leaderId));
    }

    @Override
    public PairReport synergies(StatsFilter filter, int leaderId, long minMatches) {
        return pairReport(filter, leaderId, minMatches, repository.synergies(filter, leaderId));
    }

    @Override
    public LeaderBreakdown breakdown(StatsFilter filter, int leaderId, long minMatches) {
        StatsSample sample = repository.sample(filter);
        LeaderRow own = repository.leaderRows(filter).stream()
                .filter(r -> r.leaderId() == leaderId).findFirst().orElse(null);
        LeaderStat overall = toLeaderStat(leaderId, own == null ? 0 : own.matches(), own == null ? 0 : own.wins(),
                sample.participants(), minMatches);
        return new LeaderBreakdown(sample, leaderId, minMatches, overall,
                axis(filter, leaderId, minMatches, BreakdownDimension.TEAM_SIZE),
                axis(filter, leaderId, minMatches, BreakdownDimension.GAME_MODE),
                axis(filter, leaderId, minMatches, BreakdownDimension.MAP),
                axis(filter, leaderId, minMatches, BreakdownDimension.RANK));
    }

    /** Un eje: tamaño de equipo y rango salen en su orden natural; mapas y modos, de mejor a peor. */
    private List<BreakdownStat> axis(StatsFilter filter, int leaderId, long minMatches, BreakdownDimension dimension) {
        List<BreakdownStat> stats = new ArrayList<>();
        for (BreakdownRow row : repository.breakdown(filter, leaderId, dimension)) {
            WinRate wr = new WinRate(row.matches(), row.wins());
            stats.add(new BreakdownStat(row.key(), row.matches(), row.wins(), wr.rate(), wr.low(), wr.high(),
                    wr.verdict(minMatches)));
        }
        if (dimension.hasNaturalOrder()) {
            stats.sort(Comparator.comparingInt(s -> numericKey(s.key())));
        } else {
            stats.sort(Comparator
                    .comparing((BreakdownStat s) -> s.verdict() == WinRate.Verdict.INSUFFICIENT_DATA)
                    .thenComparing(Comparator.comparingDouble(BreakdownStat::winRateLow).reversed())
                    .thenComparing(Comparator.comparingLong(BreakdownStat::matches).reversed()));
        }
        return stats;
    }

    private static int numericKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private PairReport pairReport(StatsFilter filter, int leaderId, long minMatches, List<PairRow> rows) {
        List<PairStat> pairs = new ArrayList<>();
        for (PairRow row : rows) {
            WinRate wr = new WinRate(row.matches(), row.wins());
            pairs.add(new PairStat(row.otherLeaderId(), row.matches(), row.wins(), wr.rate(), wr.low(), wr.high(),
                    wr.verdict(minMatches)));
        }
        pairs.sort(Comparator
                .comparing((PairStat s) -> s.verdict() == WinRate.Verdict.INSUFFICIENT_DATA)
                .thenComparing(Comparator.comparingDouble(PairStat::winRateLow).reversed())
                .thenComparing(Comparator.comparingLong(PairStat::matches).reversed()));
        return new PairReport(repository.sample(filter), leaderId, minMatches, pairs);
    }

    private static LeaderStat toLeaderStat(int leaderId, long matches, long wins, long participants, long minMatches) {
        WinRate wr = new WinRate(matches, wins);
        double pick = participants == 0 ? 0 : (double) matches / participants;
        return new LeaderStat(leaderId, matches, wins, wr.rate(), wr.low(), wr.high(), pick, wr.verdict(minMatches));
    }
}
