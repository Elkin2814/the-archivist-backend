package com.thearchivist.api.player.domain.ports.in;

import com.thearchivist.api.player.domain.models.PlayerSummary;

public interface GetPlayerSummaryUseCase {
    PlayerSummary execute(String gamertag);
}
