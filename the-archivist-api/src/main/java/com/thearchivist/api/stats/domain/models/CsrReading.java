package com.thearchivist.api.stats.domain.models;

/**
 * CSR de un jugador en un instante (antes o después de una partida), tal como lo entrega la Halo API.
 *
 * @param tier                        tier dentro de la designación (sube al avanzar)
 * @param designation                 ID de la designación: 0 Unranked, 1 Bronze ... 6 Onyx, 7 Champion
 * @param raw                         puntos CSR crudos; solo significativos en los rangos más altos
 * @param percentToNextTier           avance hacia el siguiente tier, de 0 a 100
 * @param measurementMatchesRemaining partidas de calibración que faltan (0 cuando ya está calibrado)
 * @param rank                        posición en el ranking global; solo existe en la cima
 */
public record CsrReading(
        Integer tier,
        Integer designation,
        Integer raw,
        Integer percentToNextTier,
        Integer measurementMatchesRemaining,
        Integer rank
) {
    /** Devuelve null cuando no hay ningún dato (por ejemplo, colas sin CSR). */
    public static CsrReading ofNullable(Integer tier, Integer designation, Integer raw, Integer percentToNextTier,
                                        Integer measurementMatchesRemaining, Integer rank) {
        if (tier == null && designation == null && raw == null && percentToNextTier == null
                && measurementMatchesRemaining == null && rank == null) {
            return null;
        }
        return new CsrReading(tier, designation, raw, percentToNextTier, measurementMatchesRemaining, rank);
    }
}
