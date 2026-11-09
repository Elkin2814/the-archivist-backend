package com.thearchivist.api.metadata.application.services;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;
import com.thearchivist.api.metadata.domain.ports.in.GetMetadataUseCase;
import com.thearchivist.api.metadata.domain.ports.in.SyncMetadataUseCase;
import com.thearchivist.api.metadata.domain.ports.out.HaloMetadataClientPort;
import com.thearchivist.api.metadata.domain.ports.out.MetadataRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MetadataService implements GetMetadataUseCase, SyncMetadataUseCase {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final HaloMetadataClientPort haloClient;
    private final MetadataRepositoryPort repository;

    public MetadataService(HaloMetadataClientPort haloClient, MetadataRepositoryPort repository) {
        this.haloClient = haloClient;
        this.repository = repository;
    }

    @Override
    public int sync() {
        List<MapInfo> maps = haloClient.fetchMaps();
        List<PlaylistInfo> playlists = haloClient.fetchPlaylists();
        List<SeasonInfo> seasons = haloClient.fetchSeasons();
        List<CsrDesignationInfo> designations = haloClient.fetchCsrDesignations();

        repository.saveMaps(maps);
        repository.savePlaylists(playlists);
        repository.saveSeasons(seasons);
        repository.saveCsrDesignations(designations);

        int total = maps.size() + playlists.size() + seasons.size() + designations.size();
        log.info("Metadata sincronizada: {} mapas, {} playlists, {} temporadas, {} rangos CSR",
                maps.size(), playlists.size(), seasons.size(), designations.size());
        return total;
    }

    @Override
    public void syncIfStale(int maxAgeDays) {
        Optional<Instant> oldest = repository.oldestSync();
        boolean stale = oldest.isEmpty()
                || oldest.get().isBefore(Instant.now().minus(Duration.ofDays(maxAgeDays)));
        if (stale) {
            sync();
        } else {
            log.info("Metadata al día (última sincronización: {}); no se vuelve a descargar", oldest.get());
        }
    }

    @Override
    public List<MapInfo> getMaps() {
        return repository.findMaps();
    }

    @Override
    public List<PlaylistInfo> getPlaylists() {
        return repository.findPlaylists();
    }

    @Override
    public List<SeasonInfo> getSeasons() {
        return repository.findSeasons();
    }

    @Override
    public List<CsrDesignationInfo> getCsrDesignations() {
        return repository.findCsrDesignations();
    }
}
