package com.thearchivist.api.leader.application.services;

import com.thearchivist.api.leader.domain.models.MostPlayedLeader;
import com.thearchivist.api.leader.domain.models.MostWinRateLeader;
import com.thearchivist.api.leader.domain.ports.in.GetMostPlayedLeaderUseCase;
import com.thearchivist.api.leader.domain.ports.in.GetMostWinRateLeaderUseCase;

import java.util.List;

public class LeaderService implements GetMostPlayedLeaderUseCase, GetMostWinRateLeaderUseCase {

    private final GetMostPlayedLeaderUseCase getMostPlayedLeaderUseCase;
    private final GetMostWinRateLeaderUseCase getMostWinRateLeaderUseCase;

    public LeaderService(GetMostPlayedLeaderUseCase getMostPlayedLeaderUseCase, GetMostWinRateLeaderUseCase getMostWinRateLeaderUseCase) {
        this.getMostPlayedLeaderUseCase = getMostPlayedLeaderUseCase;
        this.getMostWinRateLeaderUseCase = getMostWinRateLeaderUseCase;
    }

    @Override
    public List<MostPlayedLeader> getMostPlayedLeader() {
        return getMostPlayedLeaderUseCase.getMostPlayedLeader();
    }

    @Override
    public List<MostWinRateLeader> getMostWinRateLeader() {
        return getMostWinRateLeaderUseCase.getMostWinRateLeader();
    }
}
