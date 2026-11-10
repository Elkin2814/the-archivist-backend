package com.thearchivist.api.metadata.domain.ports.out;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MetadataRepositoryPort {
    void saveLeaders(List<LeaderMetadataInfo> leaders);

    void saveMaps(List<MapInfo> maps);

    void savePlaylists(List<PlaylistInfo> playlists);

    void saveSeasons(List<SeasonInfo> seasons);

    void saveCsrDesignations(List<CsrDesignationInfo> designations);

    List<LeaderMetadataInfo> findLeaders();

    List<MapInfo> findMaps();

    List<PlaylistInfo> findPlaylists();

    List<SeasonInfo> findSeasons();

    List<CsrDesignationInfo> findCsrDesignations();

    /** Fecha de la sincronización más antigua entre las tablas de Metadata; vacío si alguna no tiene datos. */
    Optional<Instant> oldestSync();
}
