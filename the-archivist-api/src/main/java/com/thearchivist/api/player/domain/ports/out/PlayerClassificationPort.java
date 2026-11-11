package com.thearchivist.api.player.domain.ports.out;

import com.thearchivist.api.player.domain.models.PlayerClassification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lecturas que necesita la clasificación de un jugador (capas 2 y 3). */
public interface PlayerClassificationPort {

    /**
     * Lo guardado del jugador en cada cola de matchmaking: partidas decididas (victoria o derrota; se excluyen las
     * caídas de servidor ya detectadas) y el rango tras la última partida con CSR. Más jugada primero.
     */
    List<PlaylistRecord> findPlaylistRecords(String gamertag);

    /** Puesto oficial del jugador en el leaderboard guardado de esa cola. */
    Optional<Integer> findLeaderboardRank(String gamertag, UUID playlistId);

    /** Dónde queda {@code mmr} entre los participantes válidos de la cola desde {@code since}. */
    Optional<Percentile> findMmrPercentile(UUID playlistId, double mmr, Instant since);

    record PlaylistRecord(UUID playlistId, String playlistName, long matches, long wins, Instant lastMatch,
                          PlayerClassification.Standing standing) {}

    /** @param value 0 a 100; {@code sample} participantes comparados */
    record Percentile(double value, long sample) {}
}
