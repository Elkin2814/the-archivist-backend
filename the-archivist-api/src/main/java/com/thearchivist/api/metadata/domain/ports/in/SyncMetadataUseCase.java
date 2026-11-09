package com.thearchivist.api.metadata.domain.ports.in;

public interface SyncMetadataUseCase {
    /** Descarga la Metadata de la Halo API y la guarda (upsert). Devuelve cuántos registros procesó. */
    int sync();

    /** Sincroniza solo si no hay datos o los guardados tienen más de {@code maxAgeDays} días. */
    void syncIfStale(int maxAgeDays);
}
