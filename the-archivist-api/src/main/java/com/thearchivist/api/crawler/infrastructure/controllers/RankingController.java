package com.thearchivist.api.crawler.infrastructure.controllers;

import com.thearchivist.api.crawler.domain.models.RankedPlayer;
import com.thearchivist.api.crawler.domain.models.RankedPlaylist;
import com.thearchivist.api.crawler.domain.models.TrackedPlayer;
import com.thearchivist.api.crawler.domain.ports.in.GetRankingsUseCase;
import com.thearchivist.api.stats.domain.models.WinRate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clasificación de jugadores: copia del leaderboard oficial de la Halo API (top 250 con CSR de 1800 o más por
 * cola y temporada) y estado del rastreo. Solo lectura.
 */
@RestController
@RequestMapping("/rankings")
public class RankingController {

    private final GetRankingsUseCase rankings;

    public RankingController(GetRankingsUseCase rankings) {
        this.rankings = rankings;
    }

    /** Colas que tienen clasificación guardada. */
    @GetMapping("/playlists")
    public List<RankedPlaylist> playlists() {
        return rankings.playlists();
    }

    /** Puestos de una cola, del primero hacia abajo; {@code tracked} = partidas del jugador que ya tenemos guardadas. */
    @GetMapping("/players")
    public PlayersPage players(@RequestParam UUID playlistId,
                               @RequestParam(defaultValue = "50") @Min(1) @Max(250) int limit,
                               @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return new PlayersPage(playlistId, rankings.countPlayers(playlistId),
                rankings.players(playlistId, limit, offset));
    }

    /**
     * Clasificación propia de los jugadores rastreados, de todos los rangos (el leaderboard oficial solo trae el
     * top 250). Se ordena por designación, CSR y MMR de su última partida; solo entran activos y con
     * {@code minMatches} partidas decididas en la cola.
     */
    @GetMapping("/tracked")
    public TrackedPage tracked(@RequestParam UUID playlistId,
                               @RequestParam(defaultValue = "10") @Min(1) int minMatches,
                               @RequestParam(defaultValue = "30") @Min(1) @Max(365) int activeDays,
                               @RequestParam(defaultValue = "50") @Min(1) @Max(250) int limit,
                               @RequestParam(defaultValue = "0") @Min(0) int offset) {
        List<TrackedLine> lines = rankings.tracked(playlistId, minMatches, activeDays, limit, offset).stream()
                .map(RankingController::toLine)
                .toList();
        return new TrackedPage(playlistId, rankings.countTracked(playlistId, minMatches, activeDays),
                minMatches, activeDays, lines, TRACKED_NOTES);
    }

    private static final List<String> TRACKED_NOTES = List.of(
            "Solo incluye jugadores que ya rastreamos; no es la población completa. El puesto es relativo a ellos.",
            "Rango y MMR son los de la última partida con CSR del jugador en la cola. Orden: designación, CSR "
                    + "(solo Onyx y Champion) y MMR.",
            "winRateLow/winRateHigh es el intervalo de Wilson al 95 %; con pocas partidas el porcentaje no es firme.");

    private static TrackedLine toLine(TrackedPlayer p) {
        WinRate wr = new WinRate(p.matches(), p.wins());
        return new TrackedLine(p.position(), p.gamertag(), p.csrDesignation(), p.designationName(), p.csrTier(),
                p.csr(), p.percentToNextTier(), p.mmr(), p.matches(), p.wins(), wr.ratePct(), wr.lowPct(),
                wr.highPct(), wr.confidence().name(), p.lastMatch());
    }

    /** Jugadores de la cola de rastreo por estado. */
    @GetMapping("/crawl-status")
    public Map<String, Long> crawlStatus() {
        return rankings.queueStatus();
    }

    public record TrackedLine(int position, String gamertag, Integer csrDesignation, String designationName,
                              Integer csrTier, Integer csr, Integer percentToNextTier, Double mmr, long matches,
                              long wins, double winRate, double winRateLow, double winRateHigh, String confidence,
                              Instant lastMatch) {}

    public record TrackedPage(UUID playlistId, long total, int minMatches, int activeDays, List<TrackedLine> players,
                              List<String> notes) {}

    public record PlayersPage(UUID playlistId, long total, List<RankedPlayer> players) {}
}
