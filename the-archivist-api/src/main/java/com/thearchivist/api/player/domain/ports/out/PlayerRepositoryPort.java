package com.thearchivist.api.player.domain.ports.out;

import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.models.PlayerSummary;
import com.thearchivist.api.stats.domain.enums.StateMatch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepositoryPort {
    Optional<Player> findByGamertag(String gamertag);

    /**
     * Guarda al jugador (con su lastSync) e inserta solo las partidas que todavía no
     * tiene registradas. Es idempotente: llamarlo dos veces con lo mismo no duplica nada.
     *
     * @return cuántas partidas eran nuevas para este jugador (0 si ya las tenía todas)
     */
    int savePlayerHistory(Player player, List<PlayerMatch> matches);

    /** Partidas del jugador, la más reciente primero. {@code playlistIds} nulo/vacío = sin filtro. */
    List<PlayerMatch> findMatches(String gamertag, int page, int size, List<UUID> playlistIds);

    long countMatches(String gamertag, List<UUID> playlistIds);

    long countMatchesByState(String gamertag, StateMatch state, List<UUID> playlistIds);

    /** Partidas de matchmaking por líder, de la más jugada a la menos jugada. */
    List<PlayerSummary.LeaderStat> findLeaderStats(String gamertag, List<UUID> playlistIds);

    /** Partidas de matchmaking por modo de juego, de la más jugada a la menos jugada. */
    List<PlayerSummary.ModeStat> findGameModeStats(String gamertag, List<UUID> playlistIds);

    /** Partidas de matchmaking por cola (playlist), de la más jugada a la menos jugada. Siempre sin filtrar. */
    List<PlayerSummary.PlaylistStat> findPlaylistStats(String gamertag);

    /** Resultados de las últimas partidas de matchmaking, la más reciente primero. */
    List<StateMatch> findRecentResults(String gamertag, int limit, List<UUID> playlistIds);

    /** Rango CSR tras la última partida clasificatoria guardada. */
    Optional<PlayerSummary.Csr> findLatestCsr(String gamertag);

    /**
     * Borra a los jugadores sin sincronizar desde {@code cutoff} (y, en cascada, sus partidas
     * en player_matches). Devuelve cuántos jugadores se eliminaron.
     */
    int deleteInactivePlayers(Instant cutoff);
}
