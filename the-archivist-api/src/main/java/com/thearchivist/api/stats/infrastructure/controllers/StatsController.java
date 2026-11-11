package com.thearchivist.api.stats.infrastructure.controllers;

import com.thearchivist.api.leader.domain.enums.Language;
import com.thearchivist.api.leader.domain.models.LeaderCatalog;
import com.thearchivist.api.stats.domain.models.StatsFilter;
import com.thearchivist.api.stats.domain.models.StatsSample;
import com.thearchivist.api.stats.domain.models.WinRate;
import com.thearchivist.api.stats.domain.ports.in.GetStatsUseCase;
import com.thearchivist.api.stats.infrastructure.controllers.dto.StatsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Estadísticas del meta sobre la base permanente (capa 3): solo partidas de matchmaking válidas, sin gamertags.
 * Todos los filtros son opcionales; {@code minSeconds} (120 por defecto) descarta las partidas que acaban
 * antes de empezar en la práctica.
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    private static final List<String> NOTES = List.of(
            "La muestra sale de las partidas de los jugadores rastreados, no de toda la población.",
            "Solo cuentan partidas válidas de matchmaking: con ganador y perdedor y sin caída de servidor. "
                    + "La victoria es la del equipo.",
            "winRateLow/winRateHigh es el intervalo de confianza de Wilson al 95 %; verdict compara con el 50 % "
                    + "esperado (INSUFFICIENT_DATA si hay menos de minMatches partidas).",
            "pickRate es la parte de los puestos de jugador de la muestra que usó ese líder.");

    private final GetStatsUseCase stats;

    public StatsController(GetStatsUseCase stats) {
        this.stats = stats;
    }

    /** Win rate y popularidad de los 16 líderes, de mejor a peor (por el extremo bajo del intervalo). */
    @GetMapping("/leaders")
    public StatsResponse.LeadersReport leaders(
            @RequestParam(required = false) UUID playlistId,
            @RequestParam(required = false) @Min(0) @Max(7) Integer minDesignation,
            @RequestParam(required = false) @Min(0) @Max(7) Integer maxDesignation,
            @RequestParam(required = false) @Min(1) @Max(3) Integer teamSize,
            @RequestParam(required = false) @Min(0) Integer gameMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "" + StatsFilter.DEFAULT_MIN_SECONDS) @Min(0) int minSeconds,
            @RequestParam(defaultValue = "30") @Min(1) long minMatches,
            @RequestParam(defaultValue = "es") String lang) {

        Language language = Language.fromCode(lang);
        StatsFilter filter = filter(playlistId, minDesignation, maxDesignation, teamSize, gameMode, from, minSeconds);
        GetStatsUseCase.LeadersReport report = stats.leaders(filter, minMatches);

        List<StatsResponse.LeaderLine> lines = report.leaders().stream()
                .map(s -> {
                    WinRate wr = new WinRate(s.matches(), s.wins());
                    return new StatsResponse.LeaderLine(leader(s.leaderId(), language), s.matches(), s.wins(),
                            WinRate.pct(s.winRate()), WinRate.pct(s.winRateLow()), WinRate.pct(s.winRateHigh()),
                            WinRate.pct(s.pickRate()), wr.confidence().name(), s.verdict().name());
                })
                .toList();
        return new StatsResponse.LeadersReport(filters(filter, from), sample(report.sample()), minMatches, lines, NOTES);
    }

    /** Cómo le va a un líder contra cada líder rival (una partida cuenta una vez por cada rival del otro equipo). */
    @GetMapping("/leaders/{leaderId}/matchups")
    public StatsResponse.PairsReport matchups(
            @PathVariable int leaderId,
            @RequestParam(required = false) UUID playlistId,
            @RequestParam(required = false) @Min(0) @Max(7) Integer minDesignation,
            @RequestParam(required = false) @Min(0) @Max(7) Integer maxDesignation,
            @RequestParam(required = false) @Min(1) @Max(3) Integer teamSize,
            @RequestParam(required = false) @Min(0) Integer gameMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "" + StatsFilter.DEFAULT_MIN_SECONDS) @Min(0) int minSeconds,
            @RequestParam(defaultValue = "30") @Min(1) long minMatches,
            @RequestParam(defaultValue = "es") String lang) {

        Language language = Language.fromCode(lang);
        StatsFilter filter = filter(playlistId, minDesignation, maxDesignation, teamSize, gameMode, from, minSeconds);
        return pairs(filter, from, language, stats.matchups(filter, leaderId, minMatches));
    }

    /** Cómo le va a un líder junto a cada líder aliado (los otros jugadores de su mismo equipo). */
    @GetMapping("/leaders/{leaderId}/synergies")
    public StatsResponse.PairsReport synergies(
            @PathVariable int leaderId,
            @RequestParam(required = false) UUID playlistId,
            @RequestParam(required = false) @Min(0) @Max(7) Integer minDesignation,
            @RequestParam(required = false) @Min(0) @Max(7) Integer maxDesignation,
            @RequestParam(required = false) @Min(1) @Max(3) Integer teamSize,
            @RequestParam(required = false) @Min(0) Integer gameMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "" + StatsFilter.DEFAULT_MIN_SECONDS) @Min(0) int minSeconds,
            @RequestParam(defaultValue = "30") @Min(1) long minMatches,
            @RequestParam(defaultValue = "es") String lang) {

        Language language = Language.fromCode(lang);
        StatsFilter filter = filter(playlistId, minDesignation, maxDesignation, teamSize, gameMode, from, minSeconds);
        return pairs(filter, from, language, stats.synergies(filter, leaderId, minMatches));
    }

    /**
     * Rendimiento de un líder de un vistazo: win rate total y partido por tamaño de equipo, modo de juego, mapa y
     * rango. Es lo que necesita la página del líder en una sola llamada. Cada valor lleva su intervalo y su veredicto.
     */
    @GetMapping("/leaders/{leaderId}/breakdown")
    public StatsResponse.BreakdownReport breakdown(
            @PathVariable int leaderId,
            @RequestParam(required = false) UUID playlistId,
            @RequestParam(required = false) @Min(0) @Max(7) Integer minDesignation,
            @RequestParam(required = false) @Min(0) @Max(7) Integer maxDesignation,
            @RequestParam(required = false) @Min(1) @Max(3) Integer teamSize,
            @RequestParam(required = false) @Min(0) Integer gameMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "" + StatsFilter.DEFAULT_MIN_SECONDS) @Min(0) int minSeconds,
            @RequestParam(defaultValue = "30") @Min(1) long minMatches,
            @RequestParam(defaultValue = "es") String lang) {

        Language language = Language.fromCode(lang);
        StatsFilter filter = filter(playlistId, minDesignation, maxDesignation, teamSize, gameMode, from, minSeconds);
        GetStatsUseCase.LeaderBreakdown report = stats.breakdown(filter, leaderId, minMatches);

        GetStatsUseCase.LeaderStat o = report.overall();
        StatsResponse.LeaderLine overall = new StatsResponse.LeaderLine(leader(leaderId, language), o.matches(),
                o.wins(), WinRate.pct(o.winRate()), WinRate.pct(o.winRateLow()), WinRate.pct(o.winRateHigh()),
                WinRate.pct(o.pickRate()), new WinRate(o.matches(), o.wins()).confidence().name(), o.verdict().name());
        return new StatsResponse.BreakdownReport(filters(filter, from), sample(report.sample()),
                leader(leaderId, language), minMatches, overall,
                lines(report.byTeamSize()), lines(report.byGameMode()), lines(report.byMap()), lines(report.byRank()),
                NOTES);
    }

    private static List<StatsResponse.BreakdownLine> lines(List<GetStatsUseCase.BreakdownStat> stats) {
        return stats.stream()
                .map(s -> new StatsResponse.BreakdownLine(s.key(), s.matches(), s.wins(), WinRate.pct(s.winRate()),
                        WinRate.pct(s.winRateLow()), WinRate.pct(s.winRateHigh()),
                        new WinRate(s.matches(), s.wins()).confidence().name(), s.verdict().name()))
                .toList();
    }

    private StatsResponse.PairsReport pairs(StatsFilter filter, LocalDate from, Language language,
                                            GetStatsUseCase.PairReport report) {
        List<StatsResponse.PairLine> lines = report.pairs().stream()
                .map(p -> new StatsResponse.PairLine(leader(p.otherLeaderId(), language), p.matches(), p.wins(),
                        WinRate.pct(p.winRate()), WinRate.pct(p.winRateLow()), WinRate.pct(p.winRateHigh()),
                        new WinRate(p.matches(), p.wins()).confidence().name(), p.verdict().name()))
                .toList();
        return new StatsResponse.PairsReport(filters(filter, from), sample(report.sample()),
                leader(report.leaderId(), language), report.minMatches(), lines, NOTES);
    }

    private static StatsFilter filter(UUID playlistId, Integer minDesignation, Integer maxDesignation,
                                      Integer teamSize, Integer gameMode, LocalDate from, int minSeconds) {
        if (minDesignation != null && maxDesignation != null && minDesignation > maxDesignation) {
            throw new IllegalArgumentException("minDesignation no puede ser mayor que maxDesignation");
        }
        return new StatsFilter(playlistId, minDesignation, maxDesignation, teamSize, gameMode,
                from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC), minSeconds);
    }

    private static StatsResponse.Filters filters(StatsFilter f, LocalDate from) {
        return new StatsResponse.Filters(f.playlistId(), f.minDesignation(), f.maxDesignation(), f.teamSize(),
                f.gameMode(), from, f.minSeconds());
    }

    private static StatsResponse.Sample sample(StatsSample s) {
        return new StatsResponse.Sample(s.matches(), s.participants(), s.oldest(), s.newest());
    }

    private static StatsResponse.LeaderRef leader(int leaderId, Language language) {
        return LeaderCatalog.findByApiId(leaderId)
                .map(l -> new StatsResponse.LeaderRef(leaderId, l.name(Language.EN), l.getFaction().name(),
                        l.name(language), l.alias(language)))
                .orElseGet(() -> new StatsResponse.LeaderRef(leaderId, null, null,
                        (language == Language.EN ? "Leader #" : "Líder #") + leaderId, null));
    }
}
