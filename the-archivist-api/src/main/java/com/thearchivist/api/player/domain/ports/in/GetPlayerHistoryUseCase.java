package com.thearchivist.api.player.domain.ports.in;

import com.thearchivist.api.player.domain.models.MatchHistory;

import java.util.List;
import java.util.UUID;

public interface GetPlayerHistoryUseCase {
    /** {@code playlistIds} nulo/vacío = todas las partidas; con valor, solo las de esa cola (modo de juego). */
    MatchHistory execute(String gamertag, int page, int size, List<UUID> playlistIds);

    default MatchHistory execute(String gamertag, int page, int size) {
        return execute(gamertag, page, size, null);
    }
}
