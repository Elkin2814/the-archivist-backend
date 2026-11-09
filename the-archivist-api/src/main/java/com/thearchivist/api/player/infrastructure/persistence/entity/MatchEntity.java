package com.thearchivist.api.player.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class MatchEntity {

    @Id
    @Column(name = "match_id")
    private UUID matchId;

    @Column(name = "match_type", nullable = false)
    private int matchType;

    @Column(name = "game_mode", nullable = false)
    private int gameMode;

    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "playlist_id")
    private UUID playlistId;

    @Column(name = "map_id")
    private String mapId;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;
}
