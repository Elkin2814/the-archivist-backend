package com.thearchivist.api.crawler.domain.ports.out;

import com.thearchivist.api.crawler.domain.models.LeaderboardEntry;

import java.util.List;
import java.util.UUID;

/** Leaderboard oficial de CSR ("Halo Wars 2 - Leaderboard - Player CSR"). */
public interface LeaderboardClientPort {

    /**
     * Los mejores jugadores de una cola y temporada, del puesto 1 hacia abajo. Lista vacía si la cola no tiene
     * leaderboard. La API entrega como máximo 250.
     */
    List<LeaderboardEntry> fetchTopPlayers(UUID seasonId, UUID playlistId, int count);
}
