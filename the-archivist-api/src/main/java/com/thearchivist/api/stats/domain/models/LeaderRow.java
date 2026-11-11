package com.thearchivist.api.stats.domain.models;

/** Conteo crudo por líder: partidas jugadas y ganadas (resultado del equipo). */
public record LeaderRow(int leaderId, long matches, long wins) {}
