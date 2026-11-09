package com.thearchivist.api.metadata.infrastructure;

import com.thearchivist.api.metadata.domain.ports.in.SyncMetadataUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, descarga la Metadata de la Halo API si la base no la tiene o está vieja.
 * Si la API falla (llave mala, sin red...), la aplicación arranca igual y solo deja el error en el log.
 */
@Component
public class MetadataStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetadataStartupRunner.class);

    private final SyncMetadataUseCase syncMetadataUseCase;
    private final boolean enabled;
    private final int refreshDays;

    public MetadataStartupRunner(SyncMetadataUseCase syncMetadataUseCase,
                                 @Value("${archivist.metadata.sync-on-startup:true}") boolean enabled,
                                 @Value("${archivist.metadata.refresh-days:30}") int refreshDays) {
        this.syncMetadataUseCase = syncMetadataUseCase;
        this.enabled = enabled;
        this.refreshDays = refreshDays;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        try {
            syncMetadataUseCase.syncIfStale(refreshDays);
        } catch (RuntimeException e) {
            log.warn("No se pudo sincronizar la Metadata al arrancar: {}", e.getMessage());
        }
    }
}
