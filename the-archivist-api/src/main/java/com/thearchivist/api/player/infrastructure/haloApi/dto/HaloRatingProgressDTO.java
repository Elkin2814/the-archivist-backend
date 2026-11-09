package com.thearchivist.api.player.infrastructure.haloApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HaloRatingProgressDTO(
        @JsonProperty("PreviousCsr") HaloCsrDTO previousCsr,
        @JsonProperty("UpdatedCsr") HaloCsrDTO updatedCsr,
        @JsonProperty("PreviousMmr") HaloMmrDTO previousMmr,
        @JsonProperty("UpdatedMmr") HaloMmrDTO updatedMmr
) {}
