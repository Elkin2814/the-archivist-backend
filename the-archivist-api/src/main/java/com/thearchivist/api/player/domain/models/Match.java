package com.thearchivist.api.player.domain.models;

import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.MatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * La partida como evento global: no pertenece a un jugador en particular.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Match {
    private UUID matchId;
    private MatchType matchType;
    private GameMode gameMode;
    private UUID seasonId;
    private UUID playlistId;
    private String mapId;
    private Instant startDate;
}
