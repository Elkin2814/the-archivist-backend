package com.thearchivist.api.leader.infrastructure.controllers.dto;

/**
 * Un líder listo para mostrar. {@code apiId} es nulo mientras su ID no esté confirmado;
 * {@code imageUrl} viene de la Metadata (CDN oficial) y es nulo si aún no se sincroniza.
 */
public record LeaderResponse(
        Integer apiId,
        String key,
        String faction,
        String name,
        String alias,
        String imageUrl
) {}
