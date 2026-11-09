package com.thearchivist.api.player.domain.ports.out;

import com.thearchivist.api.player.domain.models.PlayerMatch;

import java.util.List;

public interface HaloMatchClientPort {
    List<PlayerMatch> fetchMatchHistory(String gamertag);
}
