package com.thearchivist.api.metadata.domain.models;

/** Un líder según la Metadata API: su ID de juego (el LeaderId de las partidas), nombre, facción e imagen. */
public record LeaderMetadataInfo(int id, String name, String faction, String imageUrl) {}
