package com.thearchivist.api.player.domain.ports.in;

import com.thearchivist.api.player.domain.models.MatchHistory;

public interface GetPlayerHistoryUseCase {
    MatchHistory execute(String gamertag, int page, int size);
}
