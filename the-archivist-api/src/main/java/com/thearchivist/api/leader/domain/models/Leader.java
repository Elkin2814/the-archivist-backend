package com.thearchivist.api.leader.domain.models;

import com.thearchivist.api.leader.domain.enums.Difficulty;
import com.thearchivist.api.leader.domain.enums.Faction;

import java.util.UUID;

public class Leader {
    private UUID idLeader;
    private String name;
    private Faction faction;
    private Difficulty difficulty;
}
