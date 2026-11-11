package com.thearchivist.api.stats.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloRatingProgressDTO;

import java.util.Map;

/** Un jugador dentro de "Players" de Match Result (solo lo que usa el análisis). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloMatchPlayerDTO(
        @JsonProperty("IsHuman") Boolean human,
        @JsonProperty("TeamId") Integer teamId,
        @JsonProperty("TeamPlayerIndex") Integer teamPlayerIndex,
        @JsonProperty("LeaderId") Integer leaderId,
        @JsonProperty("PlayerCompletedMatch") Boolean completedMatch,
        @JsonProperty("TimeInMatch") String timeInMatch,
        @JsonProperty("PlayerMatchOutcome") Integer outcome,
        @JsonProperty("RatingProgress") HaloRatingProgressDTO ratingProgress,
        @JsonProperty("LeaderPowerStats") Map<String, PowerStat> leaderPowerStats
) {

    /** Un poder de líder que el jugador lanzó; la Halo API solo lista los que usó al menos una vez. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PowerStat(@JsonProperty("TimesCast") Integer timesCast) {}
}
