package com.thearchivist.api.stats.domain.models;

import java.time.Instant;

/** Tamaño de la muestra detrás de una estadística, para mostrarlo siempre junto a los porcentajes. */
public record StatsSample(long matches, long participants, Instant oldest, Instant newest) {}
