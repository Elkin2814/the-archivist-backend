package com.thearchivist.api.player.domain.models;

import com.thearchivist.api.player.domain.enums.Rank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Player {
    private String gamerTag;
    // Pendiente: se calculará con los designations de Metadata. Todavía no se persiste.
    private Rank rank;
    private Instant lastSync;
}
