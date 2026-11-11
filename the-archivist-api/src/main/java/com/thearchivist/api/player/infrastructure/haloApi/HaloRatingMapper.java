package com.thearchivist.api.player.infrastructure.haloApi;

import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloCsrDTO;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloMmrDTO;
import com.thearchivist.api.stats.domain.models.CsrReading;
import com.thearchivist.api.stats.domain.models.MmrReading;

/**
 * Traduce el bloque RatingProgress de la Halo API (el mismo en "Player Match History" y en
 * "Match Result") a los value objects del dominio.
 */
public final class HaloRatingMapper {

    private HaloRatingMapper() {
    }

    /** Nulo cuando el bloque no existe (colas sin CSR). */
    public static CsrReading toCsr(HaloCsrDTO dto) {
        if (dto == null) return null;
        return CsrReading.ofNullable(dto.tier(), dto.designation(), dto.raw(), dto.percentToNextTier(),
                dto.measurementMatchesRemaining(), dto.rank());
    }

    /** Nulo cuando el bloque no existe. */
    public static MmrReading toMmr(HaloMmrDTO dto) {
        if (dto == null) return null;
        return MmrReading.ofNullable(dto.rating(), dto.variance());
    }
}
