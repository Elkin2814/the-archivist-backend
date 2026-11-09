package com.thearchivist.api.metadata.domain.models;

import java.time.Instant;
import java.util.UUID;

public record SeasonInfo(UUID id, String name, Instant startDate) {}
