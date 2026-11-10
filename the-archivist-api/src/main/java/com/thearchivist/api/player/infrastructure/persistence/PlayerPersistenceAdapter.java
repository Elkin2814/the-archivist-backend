package com.thearchivist.api.player.infrastructure.persistence;

import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.models.PlayerSummary;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import com.thearchivist.api.player.infrastructure.persistence.entity.MatchEntity;
import com.thearchivist.api.player.infrastructure.persistence.entity.PlayerEntity;
import com.thearchivist.api.player.infrastructure.persistence.repository.MatchJpaRepository;
import com.thearchivist.api.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.thearchivist.api.player.infrastructure.persistence.repository.PlayerMatchJpaRepository;
import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.StateMatch;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador de salida hacia PostgreSQL (implementa el puerto del dominio con JPA).
 */
@Component
public class PlayerPersistenceAdapter implements PlayerRepositoryPort {

    private final PlayerJpaRepository playerJpa;
    private final MatchJpaRepository matchJpa;
    private final PlayerMatchJpaRepository playerMatchJpa;
    private final JdbcClient jdbc;

    public PlayerPersistenceAdapter(PlayerJpaRepository playerJpa,
                                    MatchJpaRepository matchJpa,
                                    PlayerMatchJpaRepository playerMatchJpa,
                                    JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.playerJpa = playerJpa;
        this.matchJpa = matchJpa;
        this.playerMatchJpa = playerMatchJpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> findByGamertag(String gamertag) {
        return playerJpa.findByGamerTag(gamertag).map(PlayerPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void savePlayerHistory(Player player, List<PlayerMatch> matches) {
        final PlayerEntity base = playerJpa.findByGamerTag(player.getGamerTag()).orElseGet(() -> {
            PlayerEntity created = new PlayerEntity();
            created.setGamerTag(player.getGamerTag());
            return created;
        });
        base.setLastSync(player.getLastSync());
        PlayerEntity playerEntity = playerJpa.save(base);

        if (matches.isEmpty()) {
            return;
        }

        // Una partida solo puede aparecer una vez por jugador (idempotencia por MatchId).
        Map<UUID, PlayerMatch> byMatchId = new LinkedHashMap<>();
        for (PlayerMatch pm : matches) {
            byMatchId.putIfAbsent(pm.getMatch().getMatchId(), pm);
        }

        Set<UUID> alreadySaved = new HashSet<>(
                playerMatchJpa.findExistingMatchIds(playerEntity.getId(), byMatchId.keySet()));
        Map<UUID, MatchEntity> knownMatches = new LinkedHashMap<>();
        for (MatchEntity known : matchJpa.findAllById(byMatchId.keySet())) {
            knownMatches.put(known.getMatchId(), known);
        }

        for (Map.Entry<UUID, PlayerMatch> entry : byMatchId.entrySet()) {
            if (alreadySaved.contains(entry.getKey())) {
                continue;
            }
            PlayerMatch pm = entry.getValue();
            MatchEntity matchEntity = knownMatches.get(entry.getKey());
            if (matchEntity == null) {
                matchEntity = matchJpa.save(PlayerPersistenceMapper.toEntity(pm.getMatch()));
            }
            playerMatchJpa.save(PlayerPersistenceMapper.toEntity(pm, playerEntity, matchEntity));
        }

        // Las filas de matches deben estar en la base antes de insertar los hechos con SQL directo.
        matchJpa.flush();
        saveParticipants(byMatchId.values());
    }

    /**
     * Guarda el hecho "este líder jugó esta partida y obtuvo este resultado" SIN gamertag.
     * Es la capa permanente para el análisis: no se borra cuando se limpia la caché de jugadores.
     * Idempotente por (partida, índice de jugador).
     */
    private void saveParticipants(Collection<PlayerMatch> matches) {
        for (PlayerMatch pm : matches) {
            if (pm.getPlayerIndex() == null) continue;
            jdbc.sql("""
                    INSERT INTO the_archivist.match_participants
                        (match_id, player_index, team_id, leader_id, outcome, completed_match,
                         duration_seconds, csr_tier_before, csr_designation_before, mmr_before)
                    VALUES (:matchId, :playerIndex, :teamId, :leaderId, :outcome, :completed,
                            :duration, :tier, :designation, :mmr)
                    ON CONFLICT (match_id, player_index) DO NOTHING
                    """)
                    .param("matchId", pm.getMatch().getMatchId())
                    .param("playerIndex", pm.getPlayerIndex())
                    .param("teamId", pm.getTeamId(), Types.INTEGER)
                    .param("leaderId", pm.getLeaderId())
                    .param("outcome", pm.getStateMatch().getCode())
                    .param("completed", pm.isCompletedMatch())
                    .param("duration", pm.getDuration() == null ? null : pm.getDuration().getSeconds(), Types.BIGINT)
                    .param("tier", pm.getPreviousTier(), Types.INTEGER)
                    .param("designation", pm.getPreviousDesignation(), Types.INTEGER)
                    .param("mmr", pm.getPreviousMmr(), Types.DOUBLE)
                    .update();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerMatch> findMatches(String gamertag, int page, int size, List<UUID> playlistIds) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> (playlistIds == null || playlistIds.isEmpty()
                        ? playerMatchJpa.findPageByPlayerId(p.getId(), PageRequest.of(page, size))
                        : playerMatchJpa.findPageByPlayerIdAndPlaylist(p.getId(), playlistIds, PageRequest.of(page, size)))
                        .stream()
                        .map(PlayerPersistenceMapper::toDomain)
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public long countMatches(String gamertag, List<UUID> playlistIds) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> playlistIds == null || playlistIds.isEmpty()
                        ? playerMatchJpa.countByPlayer(p.getId())
                        : playerMatchJpa.countByPlayerAndPlaylist(p.getId(), playlistIds))
                .orElse(0L);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMatchesByState(String gamertag, StateMatch state, List<UUID> playlistIds) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> playlistIds == null || playlistIds.isEmpty()
                        ? playerMatchJpa.countByPlayerAndOutcome(p.getId(), state.getCode())
                        : playerMatchJpa.countByPlayerAndOutcomeAndPlaylist(p.getId(), playlistIds, state.getCode()))
                .orElse(0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerSummary.LeaderStat> findLeaderStats(String gamertag, List<UUID> playlistIds) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> (playlistIds == null || playlistIds.isEmpty()
                        ? playerMatchJpa.leaderStats(p.getId())
                        : playerMatchJpa.leaderStatsByPlaylist(p.getId(), playlistIds)).stream()
                        .map(row -> new PlayerSummary.LeaderStat(((Number) row[0]).intValue(), totals(row)))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerSummary.ModeStat> findGameModeStats(String gamertag, List<UUID> playlistIds) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> (playlistIds == null || playlistIds.isEmpty()
                        ? playerMatchJpa.gameModeStats(p.getId())
                        : playerMatchJpa.gameModeStatsByPlaylist(p.getId(), playlistIds)).stream()
                        .map(row -> new PlayerSummary.ModeStat(GameMode.fromCode(((Number) row[0]).intValue()), totals(row)))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerSummary.PlaylistStat> findPlaylistStats(String gamertag) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> playerMatchJpa.playlistStats(p.getId()).stream()
                        .map(row -> new PlayerSummary.PlaylistStat((UUID) row[0], totals(row)))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StateMatch> findRecentResults(String gamertag, int limit, List<UUID> playlistIds) {
        return playerJpa.findByGamerTag(gamertag)
                .map(p -> (playlistIds == null || playlistIds.isEmpty()
                        ? playerMatchJpa.findRecentMatchmaking(p.getId(), PageRequest.of(0, limit))
                        : playerMatchJpa.findRecentMatchmakingByPlaylist(p.getId(), playlistIds, PageRequest.of(0, limit))).stream()
                        .map(pm -> StateMatch.fromOutcome(pm.getOutcome()))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerSummary.Csr> findLatestCsr(String gamertag) {
        return playerJpa.findByGamerTag(gamertag)
                .flatMap(p -> playerMatchJpa.findLatestWithCsr(p.getId(), PageRequest.of(0, 1)).stream().findFirst())
                .map(pm -> new PlayerSummary.Csr(pm.getCsrDesignationAfter(), pm.getCsrTierAfter()));
    }

    @Override
    @Transactional
    public int deleteInactivePlayers(Instant cutoff) {
        return playerJpa.deleteInactiveSince(cutoff);
    }

    /** Fila de las consultas de resumen: [clave, partidas, victorias, derrotas]. */
    private static PlayerSummary.Totals totals(Object[] row) {
        return new PlayerSummary.Totals(
                ((Number) row[1]).longValue(), ((Number) row[2]).longValue(), ((Number) row[3]).longValue());
    }
}
