package com.thearchivist.api.player.application.services;

import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerSummary;
import com.thearchivist.api.player.domain.ports.in.GetPlayerHistoryUseCase;
import com.thearchivist.api.player.domain.ports.in.GetPlayerSummaryUseCase;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetPlayerSummaryService implements GetPlayerSummaryUseCase {

    /** Cuántas partidas recientes componen la "forma" del jugador. */
    private static final int RECENT_FORM_SIZE = 10;

    private final GetPlayerHistoryUseCase getPlayerHistoryUseCase;
    private final PlayerRepositoryPort playerRepositoryPort;

    public GetPlayerSummaryService(GetPlayerHistoryUseCase getPlayerHistoryUseCase,
                                   PlayerRepositoryPort playerRepositoryPort) {
        this.getPlayerHistoryUseCase = getPlayerHistoryUseCase;
        this.playerRepositoryPort = playerRepositoryPort;
    }

    @Override
    public PlayerSummary execute(String gamertag, List<UUID> playlistIds) {
        String tag = gamertag.trim();

        // Reutiliza la sincronización del historial: si el jugador no existe o está desactualizado,
        // se trae de la Halo API antes de calcular el resumen.
        getPlayerHistoryUseCase.execute(tag, 0, 1);

        Player player = playerRepositoryPort.findByGamertag(tag)
                .orElseThrow(() -> new PlayerNotFoundException(tag));

        List<PlayerSummary.LeaderStat> byLeader = playerRepositoryPort.findLeaderStats(tag, playlistIds);
        List<PlayerSummary.ModeStat> byGameMode = playerRepositoryPort.findGameModeStats(tag, playlistIds);

        long matches = byLeader.stream().mapToLong(s -> s.totals().matches()).sum();
        long wins = byLeader.stream().mapToLong(s -> s.totals().wins()).sum();
        long losses = byLeader.stream().mapToLong(s -> s.totals().losses()).sum();

        return new PlayerSummary(
                player,
                new PlayerSummary.Totals(matches, wins, losses),
                byLeader,
                byGameMode,
                playerRepositoryPort.findPlaylistStats(tag),
                playerRepositoryPort.findRecentResults(tag, RECENT_FORM_SIZE, playlistIds),
                playerRepositoryPort.findLatestCsr(tag).orElse(null)
        );
    }
}
