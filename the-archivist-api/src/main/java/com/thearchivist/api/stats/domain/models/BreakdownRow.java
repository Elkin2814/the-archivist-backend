package com.thearchivist.api.stats.domain.models;

/** Partidas y victorias de un líder dentro de un valor de un eje (un mapa, un tamaño de equipo, un rango...). */
public record BreakdownRow(String key, long matches, long wins) {}
