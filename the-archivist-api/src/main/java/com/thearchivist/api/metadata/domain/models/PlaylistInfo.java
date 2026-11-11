package com.thearchivist.api.metadata.domain.models;

import java.util.UUID;

/** @param code identificador legible de la cola en la Metadata, por ejemplo CP_RANKED_3v3_WAR */
public record PlaylistInfo(UUID id, String name, String code, String statsClassification, Boolean teamGame,
                           Boolean hidden) {}
