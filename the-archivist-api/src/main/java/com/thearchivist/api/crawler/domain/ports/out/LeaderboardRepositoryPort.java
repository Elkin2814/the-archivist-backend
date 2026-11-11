package com.thearchivist.api.crawler.domain.ports.out;

import com.thearchivist.api.crawler.domain.models.LeaderboardEntry;
import com.thearchivist.api.crawler.domain.models.RankedPlayer;
import com.thearchivist.api.crawler.domain.models.RankedPlaylist;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Copia local (capa 2) del leaderboard oficial. */
public interface LeaderboardRepositoryPort {

    /** Reemplaza por completo la lista de esa cola y temporada. */
    void replaceEntries(UUID seasonId, UUID playlistId, List<LeaderboardEntry> entries);

    List<RankedPlaylist> findPlaylists();

    /** Clasificación de la temporada más reciente guardada para esa cola. */
    List<RankedPlayer> findPlayers(UUID playlistId, int limit, int offset);

    long countPlayers(UUID playlistId);

    /** Borra puestos guardados antes de {@code cutoff}. Devuelve cuántos. */
    int purgeStale(Instant cutoff);
}
