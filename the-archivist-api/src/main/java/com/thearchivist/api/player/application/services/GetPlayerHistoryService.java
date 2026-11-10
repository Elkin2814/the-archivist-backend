package com.thearchivist.api.player.application.services;

import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.MatchHistory;
import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.ports.in.GetPlayerHistoryUseCase;
import com.thearchivist.api.player.domain.ports.out.HaloMatchClientPort;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import com.thearchivist.api.stats.domain.enums.StateMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GetPlayerHistoryService implements GetPlayerHistoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPlayerHistoryService.class);

    private final PlayerRepositoryPort playerRepositoryPort;
    private final HaloMatchClientPort haloMatchClientPort;
    private final Duration syncTtl;

    public GetPlayerHistoryService(PlayerRepositoryPort playerRepositoryPort,
                                   HaloMatchClientPort haloMatchClientPort,
                                   @Value("${archivist.sync.ttl-minutes:15}") long ttlMinutes) {
        this.playerRepositoryPort = playerRepositoryPort;
        this.haloMatchClientPort = haloMatchClientPort;
        this.syncTtl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public MatchHistory execute(String gamertag, int page, int size, List<UUID> playlistIds) {
        String tag = gamertag.trim();

        Optional<Player> existing = playerRepositoryPort.findByGamertag(tag);
        if (existing.isEmpty() || !isSyncRecent(existing.get())) {
            sync(tag, existing);
        }

        Player player = playerRepositoryPort.findByGamertag(tag)
                .orElseThrow(() -> new PlayerNotFoundException(tag));

        List<PlayerMatch> matches = playerRepositoryPort.findMatches(tag, page, size, playlistIds);
        long total = playerRepositoryPort.countMatches(tag, playlistIds);
        long wins = playerRepositoryPort.countMatchesByState(tag, StateMatch.VICTORY, playlistIds);
        long losses = playerRepositoryPort.countMatchesByState(tag, StateMatch.DEFEAT, playlistIds);

        return new MatchHistory(player, matches, page, size, total, wins, losses);
    }

    /**
     * Trae las últimas partidas de la Halo API y guarda solo las nuevas.
     * Si la API falla pero ya hay datos guardados del jugador, se responde con lo guardado.
     */
    private void sync(String tag, Optional<Player> existing) {
        try {
            List<PlayerMatch> fetched = haloMatchClientPort.fetchMatchHistory(tag);
            Player toSave = existing.orElseGet(() -> new Player(tag, null, null));
            toSave.setLastSync(Instant.now());
            playerRepositoryPort.savePlayerHistory(toSave, fetched);
        } catch (HaloApiUnavailableException e) {
            if (existing.isEmpty()) {
                throw e;
            }
            log.warn("Halo API no disponible; se responde con los datos guardados de '{}': {}", tag, e.getMessage());
        }
    }

    private boolean isSyncRecent(Player player) {
        if (player.getLastSync() == null) return false;
        return player.getLastSync().isAfter(Instant.now().minus(syncTtl));
    }
}
