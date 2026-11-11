package com.thearchivist.api.player.infrastructure.controllers;

import com.thearchivist.api.leader.domain.enums.Language;
import com.thearchivist.api.leader.domain.models.LeaderCatalog;
import com.thearchivist.api.player.domain.models.Match;
import com.thearchivist.api.player.domain.models.MatchHistory;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.models.PlayerClassification;
import com.thearchivist.api.player.domain.models.PlayerSummary;
import com.thearchivist.api.player.domain.ports.in.GetPlayerClassificationUseCase;
import com.thearchivist.api.player.domain.ports.in.GetPlayerHistoryUseCase;
import com.thearchivist.api.player.domain.ports.in.GetPlayerSummaryUseCase;
import com.thearchivist.api.player.infrastructure.controllers.dto.MatchHistoryResponse;
import com.thearchivist.api.player.infrastructure.controllers.dto.PlayerClassificationResponse;
import com.thearchivist.api.player.infrastructure.controllers.dto.PlayerMatchDTO;
import com.thearchivist.api.player.infrastructure.controllers.dto.PlayerSummaryResponse;
import com.thearchivist.api.stats.domain.models.CsrReading;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/players")
public class PlayerController {

    /** Letras, números y espacios; entre 1 y 16 caracteres. */
    private static final String GAMERTAG_REGEX = "^[\\p{L}\\p{N} ]{1,16}$";

    private final GetPlayerHistoryUseCase getPlayerHistoryUseCase;
    private final GetPlayerSummaryUseCase getPlayerSummaryUseCase;
    private final GetPlayerClassificationUseCase getPlayerClassificationUseCase;

    public PlayerController(GetPlayerHistoryUseCase getPlayerHistoryUseCase,
                            GetPlayerSummaryUseCase getPlayerSummaryUseCase,
                            GetPlayerClassificationUseCase getPlayerClassificationUseCase) {
        this.getPlayerHistoryUseCase = getPlayerHistoryUseCase;
        this.getPlayerSummaryUseCase = getPlayerSummaryUseCase;
        this.getPlayerClassificationUseCase = getPlayerClassificationUseCase;
    }

    @GetMapping("/{gamertag}/history")
    public ResponseEntity<MatchHistoryResponse> getPlayerHistory(
            @PathVariable @Pattern(regexp = GAMERTAG_REGEX, message = "Gamertag inválido") String gamertag,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<UUID> playlistId) {

        MatchHistory history = getPlayerHistoryUseCase.execute(gamertag, page, size, playlistId);
        return ResponseEntity.ok(toResponse(history));
    }

    /**
     * Resumen del jugador: totales, rendimiento por líder y modo, forma reciente y rango.
     * Con {@code playlistId} todo se recalcula solo con esa cola; {@code byPlaylist} siempre trae todas las colas jugadas.
     */
    @GetMapping("/{gamertag}/summary")
    public ResponseEntity<PlayerSummaryResponse> getPlayerSummary(
            @PathVariable @Pattern(regexp = GAMERTAG_REGEX, message = "Gamertag inválido") String gamertag,
            @RequestParam(defaultValue = "es") String lang,
            @RequestParam(required = false) List<UUID> playlistId) {

        PlayerSummary summary = getPlayerSummaryUseCase.execute(gamertag, playlistId);
        return ResponseEntity.ok(toSummaryResponse(summary, Language.fromCode(lang)));
    }

    /**
     * Clasificación del jugador en cada cola: rango oficial y MMR exactos, puesto en el leaderboard si lo tiene,
     * win rate con intervalo de confianza y qué tan firme es el resultado.
     */
    @GetMapping("/{gamertag}/classification")
    public ResponseEntity<PlayerClassificationResponse> getPlayerClassification(
            @PathVariable @Pattern(regexp = GAMERTAG_REGEX, message = "Gamertag inválido") String gamertag) {

        PlayerClassification classification = getPlayerClassificationUseCase.execute(gamertag);
        return ResponseEntity.ok(toClassificationResponse(classification));
    }

    private static final List<String> CLASSIFICATION_NOTES = List.of(
            "El rango (designationName, tier, csr) y el MMR son los valores exactos de la última partida de la cola.",
            "mmrPercentile ubica al jugador entre los participantes de nuestra base en esa cola; es relativo a la "
                    + "muestra rastreada (que incluye muchos jugadores de alto nivel), no a toda la población.",
            "winRateLow/winRateHigh es el intervalo de Wilson al 95 %; confidence resume su ancho. "
                    + "Las caídas de servidor ya detectadas no cuentan como derrota.");

    private PlayerClassificationResponse toClassificationResponse(PlayerClassification c) {
        List<PlayerClassificationResponse.PlaylistLine> lines = c.playlists().stream()
                .map(p -> {
                    PlayerClassification.Standing s = p.standing();
                    PlayerClassificationResponse.Standing standing = s == null ? null
                            : new PlayerClassificationResponse.Standing(s.designation(), s.designationName(),
                            s.tier(), s.csr(), s.percentToNextTier(), s.measurementMatchesRemaining(),
                            s.mmr(), s.mmrVariance(), s.mmrPercentile(), s.percentileSample());
                    return new PlayerClassificationResponse.PlaylistLine(p.playlistId(), p.playlistName(),
                            p.matches(), p.wins(), p.losses(), p.winRate().ratePct(), p.winRate().lowPct(),
                            p.winRate().highPct(), p.confidence().name(), p.lastMatch(), p.activity().name(),
                            p.provisional(), p.leaderboardRank(), standing);
                })
                .toList();
        return new PlayerClassificationResponse(c.player().getGamerTag(), c.player().getLastSync(), lines,
                CLASSIFICATION_NOTES);
    }

    private PlayerSummaryResponse toSummaryResponse(PlayerSummary summary, Language language) {
        List<PlayerSummaryResponse.LeaderLine> leaders = summary.byLeader().stream()
                .map(s -> {
                    PlayerSummary.Totals t = s.totals();
                    return LeaderCatalog.findByApiId(s.leaderId())
                            .map(l -> new PlayerSummaryResponse.LeaderLine(s.leaderId(), l.name(),
                                    l.getFaction().name(), l.name(language), l.alias(language),
                                    t.matches(), t.wins(), t.losses(), t.winRate()))
                            .orElseGet(() -> new PlayerSummaryResponse.LeaderLine(s.leaderId(), null, null,
                                    (language == Language.EN ? "Leader #" : "Líder #") + s.leaderId(), null,
                                    t.matches(), t.wins(), t.losses(), t.winRate()));
                })
                .toList();

        List<PlayerSummaryResponse.ModeLine> modes = summary.byGameMode().stream()
                .map(s -> new PlayerSummaryResponse.ModeLine(s.gameMode().name(),
                        s.totals().matches(), s.totals().wins(), s.totals().losses(), s.totals().winRate()))
                .toList();

        List<PlayerSummaryResponse.PlaylistLine> playlists = summary.byPlaylist().stream()
                .map(s -> new PlayerSummaryResponse.PlaylistLine(s.playlistId(),
                        s.totals().matches(), s.totals().wins(), s.totals().losses(), s.totals().winRate()))
                .toList();

        // "W" victoria, "L" derrota, "D" empate, "?" desconocido.
        List<String> form = summary.recentForm().stream()
                .map(r -> switch (r) {
                    case VICTORY -> "W";
                    case DEFEAT -> "L";
                    case DRAW -> "D";
                    default -> "?";
                })
                .toList();

        PlayerSummary.Totals o = summary.overall();
        PlayerSummary.Csr csr = summary.currentCsr();
        return new PlayerSummaryResponse(
                summary.player().getGamerTag(),
                summary.player().getLastSync(),
                new PlayerSummaryResponse.Totals(o.matches(), o.wins(), o.losses(), o.winRate()),
                leaders,
                modes,
                playlists,
                form,
                csr == null ? null : new PlayerSummaryResponse.Csr(csr.designation(), csr.tier())
        );
    }

    private MatchHistoryResponse toResponse(MatchHistory history) {
        List<PlayerMatchDTO> matches = history.getMatches().stream()
                .map(this::toDto)
                .toList();

        int totalPages = (int) Math.ceil(history.getTotalMatches() / (double) history.getSize());

        return new MatchHistoryResponse(
                history.getPlayer().getGamerTag(),
                history.getPlayer().getLastSync(),
                new MatchHistoryResponse.Summary(
                        history.getTotalMatches(), history.getWins(), history.getLosses(), history.getWinRate()),
                new MatchHistoryResponse.Pagination(
                        history.getPage(), history.getSize(), history.getTotalMatches(), totalPages),
                matches
        );
    }

    private PlayerMatchDTO toDto(PlayerMatch pm) {
        Match match = pm.getMatch();
        return new PlayerMatchDTO(
                match.getMatchId(),
                match.getStartDate(),
                match.getMapId(),
                match.getGameMode().name(),
                match.getMatchType().name(),
                pm.getLeaderId(),
                pm.getStateMatch().name(),
                pm.getDuration() == null ? null : pm.getDuration().getSeconds(),
                csr(pm.getPreviousCsr()),
                csr(pm.getUpdatedCsr()),
                pm.getPreviousMmr() == null ? null : pm.getPreviousMmr().rating(),
                pm.getUpdatedMmr() == null ? null : pm.getUpdatedMmr().rating()
        );
    }

    /** Nulo cuando la partida no tiene CSR (por ejemplo, no clasificatoria). */
    private PlayerMatchDTO.Csr csr(CsrReading csr) {
        if (csr == null || (csr.tier() == null && csr.designation() == null)) return null;
        return new PlayerMatchDTO.Csr(csr.tier(), csr.designation(), csr.percentToNextTier());
    }
}
