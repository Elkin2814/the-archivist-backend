package com.thearchivist.api.player.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloCsrDTO(
        @JsonProperty("Tier") Integer tier,
        @JsonProperty("Designation") Integer designation
) {}
