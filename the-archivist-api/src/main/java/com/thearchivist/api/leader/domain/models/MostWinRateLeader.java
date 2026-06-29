package com.thearchivist.api.leader.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MostWinRateLeader {
    private Leader leader;
    private Integer matches;
    private Double winRate;
}
