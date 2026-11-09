package com.thearchivist.api.metadata.domain.models;

import java.util.UUID;

public record PlaylistInfo(UUID id, String name, String statsClassification, Boolean teamGame, Boolean hidden) {}
