package com.thearchivist.api.metadata.domain.models;

/** Un rango CSR (Bronce, Plata, Oro...). Los tiers dentro de cada rango se numeran en las partidas. */
public record CsrDesignationInfo(int id, String name) {}
