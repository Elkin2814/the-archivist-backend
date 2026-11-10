package com.thearchivist.api.player.infrastructure.persistence.repository;

import com.thearchivist.api.player.infrastructure.persistence.entity.PlayerMatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlayerMatchJpaRepository extends JpaRepository<PlayerMatchEntity, Long> {

    @Query("select pm.match.matchId from PlayerMatchEntity pm "
            + "where pm.player.id = :playerId and pm.match.matchId in :matchIds")
    List<UUID> findExistingMatchIds(@Param("playerId") Long playerId,
                                    @Param("matchIds") Collection<UUID> matchIds);

    @Query(value = "select pm from PlayerMatchEntity pm join fetch pm.match m "
            + "where pm.player.id = :playerId order by m.startDate desc",
            countQuery = "select count(pm) from PlayerMatchEntity pm where pm.player.id = :playerId")
    Page<PlayerMatchEntity> findPageByPlayerId(@Param("playerId") Long playerId, Pageable pageable);

    // matchType = 3 es MATCHMAKING: el resumen solo cuenta partidas competitivas (sin custom ni campaña).
    @Query("select pm.leaderId, count(pm), "
            + "sum(case when pm.outcome = 1 then 1 else 0 end), "
            + "sum(case when pm.outcome = 2 then 1 else 0 end) "
            + "from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 "
            + "group by pm.leaderId order by count(pm) desc")
    List<Object[]> leaderStats(@Param("playerId") Long playerId);

    @Query("select m.gameMode, count(pm), "
            + "sum(case when pm.outcome = 1 then 1 else 0 end), "
            + "sum(case when pm.outcome = 2 then 1 else 0 end) "
            + "from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 "
            + "group by m.gameMode order by count(pm) desc")
    List<Object[]> gameModeStats(@Param("playerId") Long playerId);

    @Query("select pm from PlayerMatchEntity pm join fetch pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 order by m.startDate desc")
    List<PlayerMatchEntity> findRecentMatchmaking(@Param("playerId") Long playerId, Pageable pageable);

    @Query("select pm from PlayerMatchEntity pm join fetch pm.match m "
            + "where pm.player.id = :playerId and pm.csrDesignationAfter is not null order by m.startDate desc")
    List<PlayerMatchEntity> findLatestWithCsr(@Param("playerId") Long playerId, Pageable pageable);

    // --- Variantes filtradas por cola (playlist) ---

    @Query(value = "select pm from PlayerMatchEntity pm join fetch pm.match m "
            + "where pm.player.id = :playerId and m.playlistId in :playlistIds order by m.startDate desc",
            countQuery = "select count(pm) from PlayerMatchEntity pm join pm.match m "
                    + "where pm.player.id = :playerId and m.playlistId in :playlistIds")
    Page<PlayerMatchEntity> findPageByPlayerIdAndPlaylist(@Param("playerId") Long playerId,
                                                          @Param("playlistIds") List<UUID> playlistIds,
                                                          Pageable pageable);

    @Query("select count(pm) from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.playlistId in :playlistIds")
    long countByPlayerAndPlaylist(@Param("playerId") Long playerId, @Param("playlistIds") List<UUID> playlistIds);

    @Query("select count(pm) from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.playlistId in :playlistIds and pm.outcome = :outcome")
    long countByPlayerAndOutcomeAndPlaylist(@Param("playerId") Long playerId,
                                            @Param("playlistIds") List<UUID> playlistIds,
                                            @Param("outcome") int outcome);

    @Query("select pm.leaderId, count(pm), "
            + "sum(case when pm.outcome = 1 then 1 else 0 end), "
            + "sum(case when pm.outcome = 2 then 1 else 0 end) "
            + "from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 and m.playlistId in :playlistIds "
            + "group by pm.leaderId order by count(pm) desc")
    List<Object[]> leaderStatsByPlaylist(@Param("playerId") Long playerId, @Param("playlistIds") List<UUID> playlistIds);

    @Query("select m.gameMode, count(pm), "
            + "sum(case when pm.outcome = 1 then 1 else 0 end), "
            + "sum(case when pm.outcome = 2 then 1 else 0 end) "
            + "from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 and m.playlistId in :playlistIds "
            + "group by m.gameMode order by count(pm) desc")
    List<Object[]> gameModeStatsByPlaylist(@Param("playerId") Long playerId, @Param("playlistIds") List<UUID> playlistIds);

    @Query("select pm from PlayerMatchEntity pm join fetch pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 and m.playlistId in :playlistIds "
            + "order by m.startDate desc")
    List<PlayerMatchEntity> findRecentMatchmakingByPlaylist(@Param("playerId") Long playerId,
                                                            @Param("playlistIds") List<UUID> playlistIds,
                                                            Pageable pageable);

    /** Una fila por cola jugada: [playlistId, partidas, victorias, derrotas]. */
    @Query("select m.playlistId, count(pm), "
            + "sum(case when pm.outcome = 1 then 1 else 0 end), "
            + "sum(case when pm.outcome = 2 then 1 else 0 end) "
            + "from PlayerMatchEntity pm join pm.match m "
            + "where pm.player.id = :playerId and m.matchType = 3 and m.playlistId is not null "
            + "group by m.playlistId order by count(pm) desc")
    List<Object[]> playlistStats(@Param("playerId") Long playerId);

    @Query("select count(pm) from PlayerMatchEntity pm where pm.player.id = :playerId")
    long countByPlayer(@Param("playerId") Long playerId);

    @Query("select count(pm) from PlayerMatchEntity pm "
            + "where pm.player.id = :playerId and pm.outcome = :outcome")
    long countByPlayerAndOutcome(@Param("playerId") Long playerId, @Param("outcome") int outcome);
}
