package com.thearchivist.api.player.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloHistoryResponse(
        @JsonProperty("Results") List<HaloMatchResultDTO> results
) {}
