package com.thearchivist.api.player.domain.ports.in;

import com.thearchivist.api.player.domain.models.PlayerClassification;

public interface GetPlayerClassificationUseCase {

    /** Sincroniza al jugador si hace falta (igual que el resumen) y lo clasifica en cada cola que haya jugado. */
    PlayerClassification execute(String gamertag);
}
