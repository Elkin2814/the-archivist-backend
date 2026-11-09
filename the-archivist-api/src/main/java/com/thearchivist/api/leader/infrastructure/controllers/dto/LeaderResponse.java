package com.thearchivist.api.leader.infrastructure.controllers.dto;

/**
 * Un líder listo para mostrar. {@code apiId} es nulo mientras su ID no esté confirmado.
 */
public record LeaderResponse(
        Integer apiId,
        String key,
        String faction,
        String name,
        String alias
) {}
