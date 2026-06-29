package com.thearchivist.api.leader.domain.ports.in;

import com.thearchivist.api.leader.domain.models.MostPlayedLeader;

import java.util.List;

public interface GetMostPlayedLeaderUseCase {
    List<MostPlayedLeader> getMostPlayedLeader();
}
