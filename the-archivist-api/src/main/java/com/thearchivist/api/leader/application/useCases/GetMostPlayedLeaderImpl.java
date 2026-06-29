package com.thearchivist.api.leader.application.useCases;

import com.thearchivist.api.leader.domain.models.MostPlayedLeader;
import com.thearchivist.api.leader.domain.ports.in.GetMostPlayedLeaderUseCase;
import com.thearchivist.api.leader.domain.ports.out.LeaderRepositoryPort;

import java.util.List;

public class GetMostPlayedLeaderImpl implements GetMostPlayedLeaderUseCase {
    private final LeaderRepositoryPort leaderRepositoryPort;

    public GetMostPlayedLeaderImpl(LeaderRepositoryPort leaderRepositoryPort) {
        this.leaderRepositoryPort = leaderRepositoryPort;
    }

    @Override
    public List<MostPlayedLeader> getMostPlayedLeader() {
        return leaderRepositoryPort.getMostPlayedLeader();
    }
}
