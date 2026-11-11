package com.thearchivist.api.stats.domain.models;

/** Conteo crudo de un líder frente a (o junto a) otro: partidas y victorias del primero. */
public record PairRow(int otherLeaderId, long matches, long wins) {}
