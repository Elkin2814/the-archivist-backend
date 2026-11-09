package com.thearchivist.api.player.infrastructure.controllers;

import com.thearchivist.api.leader.domain.enums.Language;
import com.thearchivist.api.leader.domain.models.LeaderCatalog;
import com.thearchivist.api.player.domain.models.Match;
import com.thearchivist.api.player.domain.models.MatchHistory;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.models.PlayerSummary;
import com.thearchivist.api.player.domain.ports.in.GetPlayerHistoryUseCase;
import com.thearchivist.api.player.domain.ports.in.GetPlayerSummaryUseCase;
import com.thearchivist.api.player.infrastructure.controllers.dto.MatchHistoryResponse;
import com.thearchivist.api.player.infrastructure.controllers.dto.PlayerMatchDTO;
import com.thearchivist.api.player.infrastructure.controllers.dto.PlayerSummaryResponse;
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

@RestController
@RequestMapping("/players")
public class PlayerController {

    /** Letras, números y espacios; entre 1 y 16 caracteres. */
    private static final String GAMERTAG_REGEX = "^[\\p{L}\\p{N} ]{1,16}$";

    private final GetPlayerHistoryUseCase getPlayerHistoryUseCase;
    private final GetPlayerSummaryUseCase getPlayerSummaryUseCase;

    public PlayerController(GetPlayerHistoryUseCase getPlayerHistoryUseCase,
                            GetPlayerSummaryUseCase getPlayerSummaryUseCase) {
        this.getPlayerHistoryUseCase = getPlayerHistoryUseCase;
        this.getPlayerSummaryUseCase = getPlayerSummaryUseCase;
    }

    @GetMapping("/{gamertag}/history")
    public ResponseEntity<MatchHistoryResponse> getPlayerHistory(
            @PathVariable @Pattern(regexp = GAMERTAG_REGEX, message = "Gamertag inválido") String gamertag,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        MatchHistory history = getPlayerHistoryUseCase.execute(gamertag, page, size);
        return ResponseEntity.ok(toResponse(history));
    }

    /** Resumen del jugador: totales, rendimiento por líder y modo, forma reciente y rango. */
    @GetMapping("/{gamertag}/summary")
    public ResponseEntity<PlayerSummaryResponse> getPlayerSummary(
            @PathVariable @Pattern(regexp = GAMERTAG_REGEX, message = "Gamertag inválido") String gamertag,
            @RequestParam(defaultValue = "es") String lang) {

        PlayerSummary summary = getPlayerSummaryUseCase.execute(gamertag);
        return ResponseEntity.ok(toSummaryResponse(summary, Language.fromCode(lang)));
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
                csr(pm.getPreviousTier(), pm.getPreviousDesignation()),
                csr(pm.getUpdatedTier(), pm.getUpdatedDesignation()),
                pm.getPreviousMmr(),
                pm.getUpdatedMmr()
        );
    }

    /** Nulo cuando la partida no tiene CSR (por ejemplo, no clasificatoria). */
    private PlayerMatchDTO.Csr csr(Integer tier, Integer designation) {
        if (tier == null && designation == null) return null;
        return new PlayerMatchDTO.Csr(tier, designation);
    }
}
