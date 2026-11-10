package com.thearchivist.api.metadata.domain.models;

import java.util.UUID;

/** Un mapa de la Metadata API. {@code id} es el "MapId" que aparece en las partidas. */
public record MapInfo(String id, UUID identity, String name, String imageUrl) {}
