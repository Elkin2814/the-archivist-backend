package com.thearchivist.api.crawler.domain.ports.in;

import com.thearchivist.api.crawler.domain.models.RankedPlayer;
import com.thearchivist.api.crawler.domain.models.RankedPlaylist;
import com.thearchivist.api.crawler.domain.models.TrackedPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Clasificación de jugadores (copia del leaderboard oficial) y estado de la cola de rastreo. */
public interface GetRankingsUseCase {

    List<RankedPlaylist> playlists();

    List<RankedPlayer> players(UUID playlistId, int limit, int offset);

    long countPlayers(UUID playlistId);

    /** Clasificación propia de jugadores rastreados activos (todos los rangos), con su CSR y MMR exactos. */
    List<TrackedPlayer> tracked(UUID playlistId, int minMatches, int activeDays, int limit, int offset);

    long countTracked(UUID playlistId, int minMatches, int activeDays);

    /** Jugadores de la cola por estado (PENDING, DONE, NOT_FOUND, FAILED). */
    Map<String, Long> queueStatus();
}
