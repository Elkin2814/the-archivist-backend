package com.thearchivist.api.player.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Un elemento de "Results" en "Halo Wars 2 - Player Match History".
 * Los IDs llegan como texto y se convierten a UUID en el mapper, de forma tolerante.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloMatchResultDTO(
        @JsonProperty("MatchId") String matchId,
        @JsonProperty("MatchType") Integer matchType,
        @JsonProperty("GameMode") Integer gameMode,
        @JsonProperty("SeasonId") String seasonId,
        @JsonProperty("PlaylistId") String playlistId,
        @JsonProperty("MapId") String mapId,
        @JsonProperty("MatchStartDate") HaloDateDTO matchStartDate,
        @JsonProperty("PlayerMatchDuration") String duration,
        @JsonProperty("PlayerIndex") Integer playerIndex,
        @JsonProperty("TeamId") Integer teamId,
        @JsonProperty("TeamPlayerIndex") Integer teamPlayerIndex,
        @JsonProperty("LeaderId") Integer leaderId,
        @JsonProperty("PlayerCompletedMatch") Boolean completedMatch,
        @JsonProperty("PlayerMatchOutcome") Integer outcome,
        @JsonProperty("XPProgress") HaloXpDTO xpProgress,
        @JsonProperty("RatingProgress") HaloRatingProgressDTO ratingProgress
) {}
