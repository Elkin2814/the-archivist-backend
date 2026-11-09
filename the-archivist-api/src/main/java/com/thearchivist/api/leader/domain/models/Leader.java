package com.thearchivist.api.leader.domain.models;

import com.thearchivist.api.leader.domain.enums.Difficulty;
import com.thearchivist.api.leader.domain.enums.Faction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Leader {
    // En la Halo API el LeaderId es un entero (no un UUID).
    private int idLeader;
    private String name;
    private Faction faction;
    private Difficulty difficulty;
}
