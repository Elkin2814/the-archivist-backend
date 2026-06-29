package com.thearchivist.api.leader.domain.ports.out;

import com.thearchivist.api.leader.domain.models.MostPlayedLeader;
import com.thearchivist.api.leader.domain.models.MostWinRateLeader;

import java.util.List;

public interface LeaderRepositoryPort {
    List<MostPlayedLeader> getMostPlayedLeader();
    List<MostWinRateLeader> getMostWinRateLeader();
}
