package com.thearchivist.api.leader.domain.ports.in;

import com.thearchivist.api.leader.domain.models.MostWinRateLeader;

import java.util.List;

public interface GetMostWinRateLeaderUseCase {
    List<MostWinRateLeader> getMostWinRateLeader();
}
