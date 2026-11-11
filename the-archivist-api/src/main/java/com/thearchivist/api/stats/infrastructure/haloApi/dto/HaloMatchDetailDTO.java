package com.thearchivist.api.stats.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloDateDTO;

import java.util.Map;

/**
 * Respuesta de "Halo Wars 2 - Match Result". "Players" es un diccionario cuya clave es el índice del
 * jugador en la partida ("1", "2"...), el mismo PlayerIndex que trae el historial.
 * Los gamertags (HumanPlayerId) no se declaran: Jackson los descarta y nunca llegan al dominio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloMatchDetailDTO(
        @JsonProperty("MatchId") String matchId,
        @JsonProperty("MatchType") Integer matchType,
        @JsonProperty("GameMode") Integer gameMode,
        @JsonProperty("SeasonId") String seasonId,
        @JsonProperty("PlaylistId") String playlistId,
        @JsonProperty("MapId") String mapId,
        @JsonProperty("IsMatchComplete") Boolean matchComplete,
        @JsonProperty("MatchEndReason") Integer matchEndReason,
        @JsonProperty("VictoryCondition") Integer victoryCondition,
        @JsonProperty("MatchStartDate") HaloDateDTO matchStartDate,
        @JsonProperty("MatchDuration") String matchDuration,
        @JsonProperty("Teams") Map<String, TeamDTO> teams,
        @JsonProperty("Players") Map<String, HaloMatchPlayerDTO> players
) {

    /** Del equipo solo importan el resultado y el tamaño; el resto de campos de "Teams" se descarta. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamDTO(@JsonProperty("MatchOutcome") Integer matchOutcome,
                          @JsonProperty("TeamSize") Integer teamSize) {}
}
