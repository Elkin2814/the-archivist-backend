package com.thearchivist.api.player.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_matches")
@Getter
@Setter
@NoArgsConstructor
public class PlayerMatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @Column(name = "leader_id", nullable = false)
    private int leaderId;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "player_index")
    private Integer playerIndex;

    @Column(name = "team_player_index")
    private Integer teamPlayerIndex;

    @Column(name = "completed_match", nullable = false)
    private boolean completedMatch;

    /** Código de la Halo API: 1 victoria, 2 derrota, 3 empate, 0 desconocido. */
    @Column(name = "outcome", nullable = false)
    private int outcome;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "csr_tier_before")
    private Integer csrTierBefore;

    @Column(name = "csr_designation_before")
    private Integer csrDesignationBefore;

    @Column(name = "csr_tier_after")
    private Integer csrTierAfter;

    @Column(name = "csr_designation_after")
    private Integer csrDesignationAfter;

    @Column(name = "mmr_before")
    private Double mmrBefore;

    @Column(name = "mmr_after")
    private Double mmrAfter;

    @Column(name = "xp_before")
    private Integer xpBefore;

    @Column(name = "xp_after")
    private Integer xpAfter;
}
