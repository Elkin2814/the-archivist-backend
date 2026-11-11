package com.thearchivist.api.crawler.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Respuesta de "Halo Wars 2 - Leaderboard - Player CSR". Solo se leen los campos que usamos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloLeaderboardResponse(@JsonProperty("Results") List<Item> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(@JsonProperty("Player") PlayerDTO player,
                       @JsonProperty("Rank") Integer rank,
                       @JsonProperty("Score") ScoreDTO score) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerDTO(@JsonProperty("Gamertag") String gamertag) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoreDTO(@JsonProperty("Tier") Integer tier,
                           @JsonProperty("DesignationId") Integer designationId,
                           @JsonProperty("Csr") Integer csr,
                           @JsonProperty("PercentToNextTier") Integer percentToNextTier) {}
}
