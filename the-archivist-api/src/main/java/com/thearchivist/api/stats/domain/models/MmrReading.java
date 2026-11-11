package com.thearchivist.api.stats.domain.models;

/**
 * MMR oculto de un jugador en un instante. El rating puede ser negativo; la varianza indica
 * qué tan "segura" está la estimación (alta al inicio de la temporada).
 */
public record MmrReading(Double rating, Double variance) {

    /** Devuelve null cuando no hay ningún dato. */
    public static MmrReading ofNullable(Double rating, Double variance) {
        return rating == null && variance == null ? null : new MmrReading(rating, variance);
    }
}
