package com.thearchivist.api.player.domain.ports.in;

import com.thearchivist.api.player.domain.models.PlayerSummary;

import java.util.List;
import java.util.UUID;

public interface GetPlayerSummaryUseCase {
    /** {@code playlistIds} nulo/vacío = todo el matchmaking; con valor, solo esa cola (modo de juego). */
    PlayerSummary execute(String gamertag, List<UUID> playlistIds);

    default PlayerSummary execute(String gamertag) {
        return execute(gamertag, null);
    }
}
