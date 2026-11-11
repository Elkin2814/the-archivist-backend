package com.thearchivist.api.player.domain.ports.out;

import com.thearchivist.api.player.domain.models.HistoryPage;
import com.thearchivist.api.player.domain.models.PlayerMatch;

import java.util.List;

public interface HaloMatchClientPort {

    /** Máximo que la Halo API entrega por llamada de historial. */
    int MAX_PAGE_SIZE = 25;

    /** Las últimas {@value #MAX_PAGE_SIZE} partidas del jugador (la más reciente primero). */
    List<PlayerMatch> fetchMatchHistory(String gamertag);

    /**
     * Una página del historial, de la más reciente a la más antigua.
     *
     * @param start           posición inicial (0 = la más reciente)
     * @param count           cuántas pedir; la API recorta a {@value #MAX_PAGE_SIZE}
     * @param matchmakingOnly si es true la API omite partidas personalizadas y de campaña
     */
    HistoryPage fetchHistoryPage(String gamertag, int start, int count, boolean matchmakingOnly);
}
