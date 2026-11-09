package com.thearchivist.api.player.infrastructure.persistence.repository;

import com.thearchivist.api.player.infrastructure.persistence.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PlayerJpaRepository extends JpaRepository<PlayerEntity, Long> {

    /** El gamertag no distingue mayúsculas de minúsculas. */
    @Query("select p from PlayerEntity p where lower(p.gamerTag) = lower(:gamerTag)")
    Optional<PlayerEntity> findByGamerTag(@Param("gamerTag") String gamerTag);

    /** Borra jugadores sin sincronizar desde {@code cutoff}; la base elimina sus player_matches en cascada. */
    @Modifying(clearAutomatically = true)
    @Query("delete from PlayerEntity p where p.lastSync < :cutoff "
            + "or (p.lastSync is null and p.createdAt < :cutoff)")
    int deleteInactiveSince(@Param("cutoff") Instant cutoff);
}
