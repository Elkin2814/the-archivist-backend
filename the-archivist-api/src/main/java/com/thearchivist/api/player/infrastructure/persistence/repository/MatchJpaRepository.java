package com.thearchivist.api.player.infrastructure.persistence.repository;

import com.thearchivist.api.player.infrastructure.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchJpaRepository extends JpaRepository<MatchEntity, UUID> {
}
