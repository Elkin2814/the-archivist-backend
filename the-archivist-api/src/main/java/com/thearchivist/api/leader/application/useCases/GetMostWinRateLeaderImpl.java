package com.thearchivist.api.leader.application.useCases;

import com.thearchivist.api.leader.domain.models.MostWinRateLeader;
import com.thearchivist.api.leader.domain.ports.in.GetMostWinRateLeaderUseCase;
import com.thearchivist.api.leader.domain.ports.out.LeaderRepositoryPort;

import java.util.List;

public class GetMostWinRateLeaderImpl implements GetMostWinRateLeaderUseCase {

    private final LeaderRepositoryPort leaderRepositoryPort;

    public GetMostWinRateLeaderImpl(LeaderRepositoryPort leaderRepositoryPort) {
        this.leaderRepositoryPort = leaderRepositoryPort;
    }

    @Override
    public List<MostWinRateLeader> getMostWinRateLeader() {
        return leaderRepositoryPort.getMostWinRateLeader();
    }
}
